package com.clevertap.android.sdk.pushnotification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.clevertap.android.sdk.AnalyticsManager
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.db.BaseDatabaseManager
import com.clevertap.android.sdk.db.DBAdapter
import com.clevertap.android.sdk.pushnotification.work.CTWorkManager
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Characterization coverage for the render/routing decisions in [PushProviders._createNotification]
 * and the Live Update paths it drives (Mode A factory / Mode B SDK render, silent-push gate,
 * wzrk_pid dedupe, lifecycle-event raising).
 *
 * Assertions are on delegations (renderer, analyticsManager, dbAdapter) rather than the pixel-level
 * notification build, which is Android-heavy and verified at integration level.
 */
@RunWith(RobolectricTestRunner::class)
class PushProvidersRenderRoutingTest : BaseTestCase() {

    private lateinit var dbAdapter: DBAdapter
    private lateinit var baseDatabaseManager: BaseDatabaseManager
    private lateinit var analyticsManager: AnalyticsManager
    private lateinit var renderer: INotificationRenderer

    @Before
    override fun setUp() {
        super.setUp()
        dbAdapter = mockk(relaxed = true)
        baseDatabaseManager = mockk(relaxed = true)
        every { baseDatabaseManager.loadDBAdapter(any()) } returns dbAdapter
        analyticsManager = mockk(relaxed = true)

        renderer = mockk(relaxed = true)
        every { renderer.getMessage(any()) } returns "a message"
        every { renderer.getTitle(any(), any()) } returns "a title"
    }

    @After
    fun tearDown() {
        unmockkStatic(CleverTapAPI::class)
    }

    /** Builds a real PushProviders via its internal constructor (skips provider discovery in init()). */
    private fun buildSut(
        config: CleverTapInstanceConfig = cleverTapInstanceConfig,
        theRenderer: INotificationRenderer = renderer
    ): PushProviders {
        val sut = PushProviders(
            appCtx, config, baseDatabaseManager,
            mockk<ValidationResultStack>(relaxed = true),
            analyticsManager, mockk<CTWorkManager>(relaxed = true), mockk<Clock>(relaxed = true)
        )
        sut.pushNotificationRenderer = theRenderer
        return sut
    }

    private fun baseBundle() = Bundle().apply { putString(Constants.NOTIFICATION_TAG, "true") }

    private fun createChannel(id: String) {
        val nm = appCtx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(id, "Live", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    private fun factoryNotification(channelId: String): Notification =
        NotificationCompat.Builder(appCtx, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("t")
            .build()

    @Test
    fun `no notification tag - does nothing`() {
        buildSut()._createNotification(appCtx, Bundle(), Constants.EMPTY_NOTIFICATION_ID)
        verify(exactly = 0) { renderer.getMessage(any()) }
        verify(exactly = 0) { renderer.getTitle(any(), any()) }
    }

    @Test
    fun `analytics-only config - does not render`() {
        val analyticsOnly = mockk<CleverTapInstanceConfig>(relaxed = true)
        every { analyticsOnly.isAnalyticsOnly } returns true

        buildSut(config = analyticsOnly)._createNotification(appCtx, baseBundle(), Constants.EMPTY_NOTIFICATION_ID)

        verify(exactly = 0) { renderer.getMessage(any()) }
    }

    @Test
    fun `silent push - raises viewed and does not render`() {
        val extras = baseBundle().apply { putString(Constants.WZRK_PUSH_SILENT, "true") }

        buildSut()._createNotification(appCtx, extras, Constants.EMPTY_NOTIFICATION_ID)

        verify(exactly = 1) { analyticsManager.pushNotificationViewedEvent(extras) }
        verify(exactly = 0) { renderer.getMessage(any()) }
    }

    @Test
    fun `already-rendered wzrk_pid - returns without rendering`() {
        every { dbAdapter.doesPushNotificationIdExist("pid_1") } returns true
        val extras = baseBundle().apply { putString(Constants.WZRK_PUSH_ID, "pid_1") }

        buildSut()._createNotification(appCtx, extras, Constants.EMPTY_NOTIFICATION_ID)

        verify(exactly = 0) { renderer.getMessage(any()) }
        verify(exactly = 0) { renderer.getTitle(any(), any()) }
    }

    @Test
    fun `empty-message ordinary push - stored as silent`() {
        every { renderer.getMessage(any()) } returns ""
        buildSut()._createNotification(appCtx, baseBundle(), Constants.EMPTY_NOTIFICATION_ID)

        verify(exactly = 1) { dbAdapter.storeUninstallTimestamp() }
        verify(exactly = 0) { renderer.getTitle(any(), any()) }
    }

    @Test
    fun `Mode A with factory - renders and raises Started`() {
        createChannel("la_ch")
        mockkStatic(CleverTapAPI::class)
        val factory = mockk<ICleverTapNotificationFactory>()
        every { CleverTapAPI.getNotificationFactory() } returns factory
        every { factory.onCreateNotification(any(), any()) } returns factoryNotification("la_ch")

        val extras = baseBundle().apply {
            putString(Constants.WZRK_LIVE_ACTIVITY, "true")
            putString(Constants.WZRK_LIVE_ACTIVITY_ID, "order_1")
            putString(Constants.WZRK_LIVE_ACTIVITY_EVENT, Constants.WZRK_LIVE_ACTIVITY_EVENT_START)
            putString("wzrk_cid", "la_ch")
        }

        buildSut()._createNotification(appCtx, extras, Constants.EMPTY_NOTIFICATION_ID)

        verify(exactly = 1) { factory.onCreateNotification(appCtx, extras) }
        verify(exactly = 1) {
            analyticsManager.raiseLiveActivityLifecycleEvent(extras, Constants.LIVE_ACTIVITY_STATE_STARTED)
        }
        // Mode A is rendered entirely by the factory; the SDK renderer is not consulted.
        verify(exactly = 0) { renderer.getMessage(any()) }
    }

    @Test
    fun `Mode A factory returns null - does not raise lifecycle`() {
        mockkStatic(CleverTapAPI::class)
        val factory = mockk<ICleverTapNotificationFactory>()
        every { CleverTapAPI.getNotificationFactory() } returns factory
        every { factory.onCreateNotification(any(), any()) } returns null

        val extras = baseBundle().apply {
            putString(Constants.WZRK_LIVE_ACTIVITY, "true")
            putString(Constants.WZRK_LIVE_ACTIVITY_ID, "order_1")
        }

        buildSut()._createNotification(appCtx, extras, Constants.EMPTY_NOTIFICATION_ID)

        verify(exactly = 0) { analyticsManager.raiseLiveActivityLifecycleEvent(any(), any()) }
    }

    @Test
    fun `Mode A end event - raises Ended`() {
        createChannel("la_ch")
        mockkStatic(CleverTapAPI::class)
        val factory = mockk<ICleverTapNotificationFactory>()
        every { CleverTapAPI.getNotificationFactory() } returns factory
        every { factory.onCreateNotification(any(), any()) } returns factoryNotification("la_ch")

        val extras = baseBundle().apply {
            putString(Constants.WZRK_LIVE_ACTIVITY, "true")
            putString(Constants.WZRK_LIVE_ACTIVITY_ID, "order_1")
            putString(Constants.WZRK_LIVE_ACTIVITY_EVENT, Constants.WZRK_LIVE_ACTIVITY_EVENT_END)
            putString("wzrk_cid", "la_ch")
        }

        buildSut()._createNotification(appCtx, extras, Constants.EMPTY_NOTIFICATION_ID)

        verify(exactly = 1) {
            analyticsManager.raiseLiveActivityLifecycleEvent(extras, Constants.LIVE_ACTIVITY_STATE_ENDED)
        }
    }

    @Test
    fun `Mode A no factory - empty-message core renderer is silent (not blank render)`() {
        mockkStatic(CleverTapAPI::class)
        every { CleverTapAPI.getNotificationFactory() } returns null

        // Real core renderer: getMessage reads nm (absent -> empty), so the empty-message gate applies.
        val coreRenderer = CoreNotificationRenderer()
        val extras = baseBundle().apply {
            putString(Constants.WZRK_LIVE_ACTIVITY, "true")
            putString(Constants.WZRK_LIVE_ACTIVITY_ID, "order_1")
        }

        buildSut(theRenderer = coreRenderer)._createNotification(appCtx, extras, Constants.EMPTY_NOTIFICATION_ID)

        // Falls through to SDK render with the core renderer + empty message => treated as silent.
        verify(exactly = 1) { dbAdapter.storeUninstallTimestamp() }
        verify(exactly = 0) { analyticsManager.raiseLiveActivityLifecycleEvent(any(), any()) }
    }

    @Test
    fun `Mode B pt_id with empty message - not silent, proceeds to render`() {
        mockkStatic(CleverTapAPI::class)
        every { CleverTapAPI.getNotificationFactory() } returns null
        every { renderer.getMessage(any()) } returns "" // title-only pt_progress

        val extras = baseBundle().apply {
            putString(Constants.WZRK_LIVE_ACTIVITY, "true")
            putString(Constants.WZRK_LIVE_ACTIVITY_ID, "order_1")
            putString("pt_id", "pt_progress")
        }

        buildSut()._createNotification(appCtx, extras, Constants.EMPTY_NOTIFICATION_ID)

        // Mock renderer is not a CoreNotificationRenderer -> self-validating template render exemption.
        verify(exactly = 0) { dbAdapter.storeUninstallTimestamp() }
        verify(atLeast = 1) { renderer.getTitle(any(), any()) }
    }
}
