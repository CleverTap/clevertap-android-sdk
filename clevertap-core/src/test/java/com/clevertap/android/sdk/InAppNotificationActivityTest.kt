package com.clevertap.android.sdk

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.clevertap.android.sdk.inapp.CTInAppNotification
import com.clevertap.android.sdk.inapp.InAppController
import com.clevertap.android.sdk.inapp.InAppFixtures
import com.clevertap.android.sdk.inapp.fragment.CTInAppHtmlInterstitialFragment
import com.clevertap.android.sdk.inapp.fragment.CTInAppNativeHeaderFragment
import com.clevertap.android.sdk.utils.configMock
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import android.os.Looper
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class InAppNotificationActivityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var config: CleverTapInstanceConfig
    private val mockInAppController = mockk<InAppController>(relaxed = true)

    private val htmlInApp = """
        {
          "ti": 1120,
          "wzrk_id": "1120_test",
          "type": "custom-html",
          "w": { "dk": true, "sc": true, "pos": "c", "xp": 90, "yp": 85 },
          "d": { "html": "<html><body>in-app</body></html>" }
        }
    """.trimIndent()

    @Before
    fun setUp() {
        config = CleverTapInstanceConfig.createInstance(context, "TEST", "TEST")
        mockkStatic(CleverTapAPI::class)
        val coreState = mockk<CoreState>(relaxed = true)
        every { coreState.inAppController } returns mockInAppController
        val cleverTapAPI = mockk<CleverTapAPI>(relaxed = true)
        every { cleverTapAPI.coreState } returns coreState
        every { CleverTapAPI.instanceWithConfig(any(), any()) } returns cleverTapAPI
        // By default nothing is saved, so an in-app that is not in memory cannot be restored.
        every { mockInAppController.restoreDisplayedInApp(any(), any()) } answers {
            secondArg<InAppController.RestoredInAppListener>().onRestoreResult(null)
        }
        mockkObject(InAppController.Companion)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun inAppIntent(inAppId: String) =
        Intent(context, InAppNotificationActivity::class.java)
            .putExtra(Constants.INAPP_ID_KEY, inAppId)
            .putExtra("config", config)

    @Test
    fun `launchForInAppNotification sends only the in-app id and not the in-app object`() {
        val mockContext = mockk<Context>(relaxed = true)
        every { mockContext.packageName } returns "com.clevertap.test"
        val intent = slot<Intent>()
        every { mockContext.startActivity(capture(intent)) } just runs
        val inApp = CTInAppNotification(JSONObject(InAppFixtures.TYPE_HALF_INTERSTITIAL), true)

        InAppNotificationActivity.launchForInAppNotification(mockContext, inApp, configMock())

        val extras = intent.captured.extras!!
        assertEquals("1742963524_20250326", extras.getString(Constants.INAPP_ID_KEY))
        // The whole in-app must never go through Binder again (SDK-6299).
        assertFalse(extras.containsKey(Constants.INAPP_KEY))
    }

    @Test
    fun `shows the in-app fragment when the in-app is in memory`() {
        val inApp = CTInAppNotification(JSONObject(htmlInApp), true)
        every { InAppController.getDisplayingInApp("1120_test") } returns inApp

        val activity = Robolectric.buildActivity(InAppNotificationActivity::class.java, inAppIntent("1120_test"))
            .setup().get()

        assertFalse(activity.isFinishing)
        assertTrue(activity.supportFragmentManager.fragments.any { it is CTInAppHtmlInterstitialFragment })
    }

    @Test
    fun `finishes without a fragment when the in-app is not in memory`() {
        every { InAppController.getDisplayingInApp(any()) } returns null

        val activity = Robolectric.buildActivity(InAppNotificationActivity::class.java, inAppIntent("1120_test"))
            .create().get()

        assertTrue(activity.isFinishing)
        assertTrue(activity.supportFragmentManager.fragments.isEmpty())
    }

    @Test
    fun `after process death the restored fragment removes itself and the activity finishes`() {
        // 1. In-app on screen, Android saves the activity state (e.g. user presses Home).
        val inApp = CTInAppNotification(JSONObject(htmlInApp), true)
        every { InAppController.getDisplayingInApp("1120_test") } returns inApp
        val first = Robolectric.buildActivity(InAppNotificationActivity::class.java, inAppIntent("1120_test"))
            .setup()
        val savedState = Bundle()
        first.saveInstanceState(savedState)

        // 2. The process is killed, so memory is empty. Android recreates the activity from saved state.
        every { InAppController.getDisplayingInApp(any()) } returns null
        val recreated = Robolectric.buildActivity(InAppNotificationActivity::class.java, inAppIntent("1120_test"))
            .create(savedState).get()
        // Android restores the old fragment inside super.onCreate; it removes itself (orphan).
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(recreated.isFinishing)
        assertTrue(recreated.supportFragmentManager.fragments.isEmpty())
    }

    @Test
    fun `header fragment removes itself when its in-app is not in memory`() {
        every { InAppController.getDisplayingInApp(any()) } returns null
        val host = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val inApp = CTInAppNotification(JSONObject(InAppFixtures.TYPE_HALF_INTERSTITIAL), true)
        val fragment = CTInAppNativeHeaderFragment().apply { setArguments(inApp, config) }

        host.supportFragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commitNow()
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(host.supportFragmentManager.fragments.isEmpty())
        assertEquals(null, fragment.view)
    }

    @Test
    fun `after process death the in-app is restored from storage and shown`() {
        // 1. In-app on screen, Android saves the activity state (e.g. user presses Home).
        val inApp = CTInAppNotification(JSONObject(htmlInApp), true)
        every { InAppController.getDisplayingInApp("1120_test") } returns inApp
        val first = Robolectric.buildActivity(InAppNotificationActivity::class.java, inAppIntent("1120_test"))
            .setup()
        val savedState = Bundle()
        first.saveInstanceState(savedState)

        // 2. The process is killed: memory is empty, but the in-app was saved, so it is restored.
        every { InAppController.getDisplayingInApp(any()) } returns null
        val restoredInApp = CTInAppNotification(JSONObject(htmlInApp), true).apply { isRestored = true }
        var shown = false
        every { mockInAppController.restoreDisplayedInApp("1120_test", any()) } answers {
            // The controller puts the restored in-app in memory before it calls back.
            every { InAppController.getDisplayingInApp("1120_test") } returns restoredInApp
            shown = secondArg<InAppController.RestoredInAppListener>().onRestoreResult(restoredInApp)
        }
        val recreated = Robolectric.buildActivity(InAppNotificationActivity::class.java, inAppIntent("1120_test"))
            .create(savedState).start().resume().get()
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(shown)
        assertFalse(recreated.isFinishing)
        assertEquals(1, recreated.supportFragmentManager.fragments.count { it is CTInAppHtmlInterstitialFragment })
    }

    @Test
    fun `activity closes when restoring the in-app takes too long`() {
        every { InAppController.getDisplayingInApp(any()) } returns null
        every { mockInAppController.restoreDisplayedInApp(any(), any()) } just runs // never calls back

        val activity = Robolectric.buildActivity(InAppNotificationActivity::class.java, inAppIntent("1120_test"))
            .create().start().resume().get()
        assertFalse(activity.isFinishing)

        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(5))

        assertTrue(activity.isFinishing)
    }

    @Test
    @Config(qualifiers = "land")
    fun `restored portrait-only in-app in landscape closes the activity and frees the in-app slot`() {
        every { InAppController.getDisplayingInApp(any()) } returns null
        val portraitOnly = CTInAppNotification(
            JSONObject(InAppFixtures.TYPE_HALF_INTERSTITIAL)
                .put(Constants.KEY_PORTRAIT, true)
                .put(Constants.KEY_LANDSCAPE, false),
            true
        ).apply { isRestored = true }
        var shown: Boolean? = null
        every { mockInAppController.restoreDisplayedInApp(any(), any()) } answers {
            every { InAppController.getDisplayingInApp(any()) } returns portraitOnly
            shown = secondArg<InAppController.RestoredInAppListener>().onRestoreResult(portraitOnly)
        }

        val activity = Robolectric.buildActivity(InAppNotificationActivity::class.java, inAppIntent("1742963524_20250326"))
            .create().get()

        // setUp dismissed it, so the controller frees the in-app slot on the dismiss path and the
        // result is true: the controller must not free it a second time.
        assertEquals(true, shown)
        assertTrue(activity.isFinishing)
        verify(exactly = 1) { mockInAppController.inAppNotificationDidDismiss(portraitOnly, null) }
    }

    @Test
    fun `restore result after the user left the screen closes it and keeps the saved in-app`() {
        every { InAppController.getDisplayingInApp(any()) } returns null
        val listener = slot<InAppController.RestoredInAppListener>()
        every { mockInAppController.restoreDisplayedInApp(any(), capture(listener)) } just runs
        val controller = Robolectric.buildActivity(InAppNotificationActivity::class.java, inAppIntent("1120_test"))
            .create().start().resume()
        // User presses Home while the restore is still running: Android saves the state.
        controller.pause().saveInstanceState(Bundle()).stop()

        val restored = CTInAppNotification(JSONObject(htmlInApp), true).apply { isRestored = true }
        val shown = listener.captured.onRestoreResult(restored)

        // A fragment cannot be added after the state is saved, so the screen closes and the
        // controller frees the slot. Nothing is dismissed, so the saved row is kept.
        assertFalse(shown)
        assertTrue(controller.get().isFinishing)
        verify(exactly = 0) { mockInAppController.inAppNotificationDidDismiss(any(), any()) }
    }

    @Test
    fun `restore result that comes after the timeout is dropped`() {
        every { InAppController.getDisplayingInApp(any()) } returns null
        val listener = slot<InAppController.RestoredInAppListener>()
        every { mockInAppController.restoreDisplayedInApp(any(), capture(listener)) } just runs
        val activity = Robolectric.buildActivity(InAppNotificationActivity::class.java, inAppIntent("1120_test"))
            .create().start().resume().get()
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(5))

        val late = CTInAppNotification(JSONObject(htmlInApp), true).apply { isRestored = true }
        // false tells the controller to free the in-app slot, since nothing shows it.
        assertFalse(listener.captured.onRestoreResult(late))
        assertTrue(activity.isFinishing)
        assertTrue(activity.supportFragmentManager.fragments.isEmpty())
    }

    @Test
    fun `restore is not started and the activity closes when the intent has no config`() {
        every { InAppController.getDisplayingInApp(any()) } returns null
        val intent = Intent(context, InAppNotificationActivity::class.java)
            .putExtra(Constants.INAPP_ID_KEY, "1120_test")

        val activity = Robolectric.buildActivity(InAppNotificationActivity::class.java, intent).create().get()

        assertTrue(activity.isFinishing)
        verify(exactly = 0) { mockInAppController.restoreDisplayedInApp(any(), any()) }
    }
}
