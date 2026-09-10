package com.clevertap.android.sdk.pushnotification

import android.content.Context
import android.os.Bundle
import com.clevertap.android.sdk.AnalyticsManager
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
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Covers the empty-message gate in [PushProviders._createNotification]: a Live Update (`wzrk_la`)
 * push is a render request validated by its own template validator (message optional), so an empty
 * message must NOT drop it as a silent push — whereas an ordinary empty-message push still is silent.
 */
@RunWith(RobolectricTestRunner::class)
class PushProvidersLiveActivityTest : BaseTestCase() {

    private lateinit var baseDatabaseManager: BaseDatabaseManager
    private lateinit var dbAdapter: DBAdapter
    private lateinit var renderer: INotificationRenderer
    private lateinit var pushProviders: PushProviders

    @Before
    override fun setUp() {
        super.setUp()

        dbAdapter = mockk(relaxed = true)
        baseDatabaseManager = mockk(relaxed = true)
        every { baseDatabaseManager.loadDBAdapter(any()) } returns dbAdapter

        // Renderer with an empty message (title present) — the title-only progress case.
        renderer = mockk(relaxed = true)
        every { renderer.getMessage(any()) } returns ""
        every { renderer.getTitle(any(), any()) } returns "Order #A1234"

        // Build a real PushProviders via its private constructor (skips provider discovery in init()).
        val ctor = PushProviders::class.java.getDeclaredConstructor(
            Context::class.java,
            CleverTapInstanceConfig::class.java,
            BaseDatabaseManager::class.java,
            ValidationResultStack::class.java,
            AnalyticsManager::class.java,
            CTWorkManager::class.java,
            Clock::class.java
        )
        ctor.isAccessible = true
        pushProviders = ctor.newInstance(
            appCtx,
            cleverTapInstanceConfig,
            baseDatabaseManager,
            mockk<ValidationResultStack>(relaxed = true),
            mockk<AnalyticsManager>(relaxed = true),
            mockk<CTWorkManager>(relaxed = true),
            mockk<Clock>(relaxed = true)
        )
        pushProviders.setPushNotificationRenderer(renderer)
    }

    @Test
    fun `empty-message ordinary push is treated as a silent push and not rendered`() {
        val extras = Bundle().apply {
            putString(Constants.NOTIFICATION_TAG, "true")
        }

        pushProviders._createNotification(appCtx, extras, Constants.EMPTY_NOTIFICATION_ID)

        // Silent path: uninstall timestamp stored, render never started (getTitle not reached).
        verify(exactly = 1) { dbAdapter.storeUninstallTimestamp() }
        verify(exactly = 0) { renderer.getTitle(any(), any()) }
    }

    @Test
    fun `empty-message Live Update is not dropped as silent and proceeds to render`() {
        val extras = Bundle().apply {
            putString(Constants.NOTIFICATION_TAG, "true")
            putString(Constants.WZRK_LIVE_ACTIVITY, "true")
            putString(Constants.WZRK_LIVE_ACTIVITY_ID, "order_A1234")
        }

        pushProviders._createNotification(appCtx, extras, Constants.EMPTY_NOTIFICATION_ID)

        // Not treated as silent, and rendering was entered (title read past the empty-message gate).
        verify(exactly = 0) { dbAdapter.storeUninstallTimestamp() }
        verify(atLeast = 1) { renderer.getTitle(any(), any()) }
    }
}
