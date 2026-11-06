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
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import android.os.Looper
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class InAppNotificationActivityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var config: CleverTapInstanceConfig

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
        every { CleverTapAPI.instanceWithConfig(any(), any()) } returns mockk(relaxed = true)
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
}
