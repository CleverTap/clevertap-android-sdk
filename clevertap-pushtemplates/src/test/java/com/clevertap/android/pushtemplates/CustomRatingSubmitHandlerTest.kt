package com.clevertap.android.pushtemplates

import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.clevertap.android.sdk.Constants
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Covers what happens on the submit tap: the event and its properties, which destination is opened,
 * and whether the notification is dismissed or swapped to its confirmation state.
 *
 * The event itself is stubbed out — raising it needs a live CleverTap instance — but the properties
 * handed to it are captured and asserted, which is the part this template owns.
 */
@RunWith(RobolectricTestRunner::class)
class CustomRatingSubmitHandlerTest {

    private val application: Application get() = RuntimeEnvironment.getApplication()
    private val context: Context get() = application
    private val notificationManager: NotificationManager
        get() = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Each test needs its own notification id, and the counter has to outlive the test instance:
     * the repeat-tap guard is process-wide by design, so an id reused by the next test would be
     * treated as an already-submitted rating.
     */
    private fun freshNotificationId() = nextNotificationId++

    private companion object {

        var nextNotificationId = 1000
    }

    @Before
    fun setUp() {
        mockkStatic(Utils::class)
        every { Utils.raiseCleverTapEvent(any(), any(), any<String>(), any()) } answers { }
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun extras(
        notificationId: Int,
        selectedPosition: Int,
        style: String = "icon",
        count: Int = 5,
        vararg extra: Pair<String, String>
    ) = Bundle().apply {
        putString(PTConstants.PT_ID, "pt_custom_rating")
        putString(PTConstants.PT_TITLE, "How did we do?")
        putString(PTConstants.PT_MSG, "Tell us")
        putString(PTConstants.PT_DEFAULT_DL, "ctdemo://home")
        putString(PTConstants.PT_RATING_STYLE, style)
        putString(PTConstants.PT_RATING_COUNT, count.toString())
        putString(PTConstants.PT_RATING_CTA_LABEL, "Submit")
        putString(PTConstants.PT_RATING_CTA_DL, "ctdemo://feedback")
        for (position in 1..count) {
            putString("${PTConstants.PT_RATING_ICON_PREFIX}$position", "https://cdn.example.com/i$position.png")
        }
        extra.forEach { (key, value) -> putString(key, value) }
        putInt(PTConstants.PT_NOTIF_ID, notificationId)
        putInt(PTConstants.PT_RATING_SELECTED_POSITION, selectedPosition)
        putBoolean(PTConstants.PT_RATING_SUBMIT, true)
    }

    private fun startedDeepLink(): String? =
        shadowOf(application).nextStartedActivity?.dataString

    private fun capturedEventProperties(): Map<String, Any> {
        val props = slot<HashMap<String, Any>>()
        verify {
            Utils.raiseCleverTapEvent(
                any(), any(), PTConstants.PT_RATING_EVENT_NAME, capture(props)
            )
        }
        return props.captured
    }

    /** Posts a rating notification so the confirmation path has something to re-render. */
    private fun postRatingNotification(notificationId: Int): Notification {
        val big = RemoteViews(context.packageName, R.layout.custom_rating)
        val notification = NotificationCompat.Builder(context, "pt_test")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setCustomContentView(RemoteViews(context.packageName, R.layout.custom_rating))
            .setCustomBigContentView(big)
            .build()
        notificationManager.notify(notificationId, notification)
        return notification
    }

    private fun activeNotification(notificationId: Int): Notification? =
        notificationManager.activeNotifications.firstOrNull { it.id == notificationId }?.notification

    @Test
    fun `submitting raises the event once, opens the destination and clears the notification`() {
        val id = freshNotificationId()
        postRatingNotification(id)

        CustomRatingSubmitHandler.submit(context, extras(id, selectedPosition = 4))

        verify(exactly = 1) {
            Utils.raiseCleverTapEvent(any(), any(), PTConstants.PT_RATING_EVENT_NAME, any())
        }
        assertEquals("ctdemo://feedback", startedDeepLink())
        assertNull(activeNotification(id))
    }

    @Test
    fun `the event carries the position, the scale and the style`() {
        CustomRatingSubmitHandler.submit(
            context, extras(freshNotificationId(), selectedPosition = 2, style = "text", count = 3).apply {
                putString("${PTConstants.PT_RATING_LABEL_PREFIX}1", "Bad")
                putString("${PTConstants.PT_RATING_LABEL_PREFIX}2", "Okay")
                putString("${PTConstants.PT_RATING_LABEL_PREFIX}3", "Great")
            }
        )

        val props = capturedEventProperties()
        assertEquals(2, props[PTConstants.PT_RATING_EVENT_VALUE])
        assertEquals(3, props[PTConstants.PT_RATING_EVENT_SCALE])
        assertEquals("text", props[PTConstants.PT_RATING_EVENT_STYLE])
        assertEquals("${PTConstants.PT_RATING_C2A_KEY}2", props[Constants.KEY_C2A])
    }

    @Test
    fun `a per-position deep link overrides the submit destination`() {
        val payload = extras(freshNotificationId(), selectedPosition = 1).apply {
            putString("pt_dl1", "ctdemo://feedback/bad")
            putString("pt_dl5", "ctdemo://review/store")
        }

        CustomRatingSubmitHandler.submit(context, payload)

        assertEquals("ctdemo://feedback/bad", startedDeepLink())
    }

    @Test
    fun `a position without an override falls through to the submit destination`() {
        val payload = extras(freshNotificationId(), selectedPosition = 3).apply {
            putString("pt_dl1", "ctdemo://feedback/bad")
        }

        CustomRatingSubmitHandler.submit(context, payload)

        assertEquals("ctdemo://feedback", startedDeepLink())
    }

    @Test
    fun `submitting with no selection does nothing at all`() {
        val id = freshNotificationId()
        postRatingNotification(id)

        CustomRatingSubmitHandler.submit(context, extras(id, selectedPosition = 0))

        verify(exactly = 0) {
            Utils.raiseCleverTapEvent(any(), any(), any<String>(), any())
        }
        assertNull(startedDeepLink())
        assertNotNull("the notification must stay in the tray", activeNotification(id))
    }

    @Test
    fun `a repeat submit on the same notification is ignored`() {
        val id = freshNotificationId()
        val payload = extras(id, selectedPosition = 2)

        CustomRatingSubmitHandler.submit(context, payload)
        shadowOf(application).nextStartedActivity // drain the first launch
        CustomRatingSubmitHandler.submit(context, extras(id, selectedPosition = 2))

        verify(exactly = 1) {
            Utils.raiseCleverTapEvent(any(), any(), PTConstants.PT_RATING_EVENT_NAME, any())
        }
        assertNull("the second tap must not open anything", startedDeepLink())
    }

    @Test
    fun `a confirmation message keeps the notification and drops the interactive views`() {
        val id = freshNotificationId()
        postRatingNotification(id)

        CustomRatingSubmitHandler.submit(
            context,
            extras(id, selectedPosition = 5)
                .apply { putString(PTConstants.PT_RATING_CONFIRM_MSG, "Thanks for rating us!") }
        )

        val confirmation = activeNotification(id)
        assertNotNull("the confirmation state stays in the tray", confirmation)

        val root = confirmation!!.bigContentView.apply(context, FrameLayout(context)) as ViewGroup
        assertEquals(View.GONE, root.findViewById<View>(R.id.custom_rating_row).visibility)
        assertEquals(View.GONE, root.findViewById<View>(R.id.custom_rating_cta).visibility)
        assertEquals(
            "Thanks for rating us!",
            root.findViewById<android.widget.TextView>(R.id.msg).text.toString()
        )
    }

    @Test
    fun `the destination is still opened when a confirmation message is configured`() {
        val id = freshNotificationId()
        postRatingNotification(id)

        CustomRatingSubmitHandler.submit(
            context,
            extras(id, selectedPosition = 1)
                .apply { putString(PTConstants.PT_RATING_CONFIRM_MSG, "Thanks!") }
        )

        assertEquals("ctdemo://feedback", startedDeepLink())
    }

    @Test
    fun `a confirmation message on a notification the user already cleared just dismisses`() {
        val id = freshNotificationId()

        CustomRatingSubmitHandler.submit(
            context,
            extras(id, selectedPosition = 1)
                .apply { putString(PTConstants.PT_RATING_CONFIRM_MSG, "Thanks!") }
        )

        assertNull(activeNotification(id))
        assertEquals("ctdemo://feedback", startedDeepLink())
    }

    @Test
    fun `internal selection markers are stripped from the intent the app receives`() {
        CustomRatingSubmitHandler.submit(context, extras(freshNotificationId(), selectedPosition = 2))

        val launched = shadowOf(application).nextStartedActivity
        assertTrue(launched.getIntExtra(PTConstants.PT_RATING_SELECTED_POSITION, -1) == -1)
        assertTrue(!launched.getBooleanExtra(PTConstants.PT_RATING_SUBMIT, false))
        assertEquals("ctdemo://feedback", launched.getStringExtra(Constants.DEEP_LINK_KEY))
    }

    @Test
    fun `a campaign with no destination at all falls back to the launcher activity`() {
        val payload = extras(freshNotificationId(), selectedPosition = 1).apply {
            remove(PTConstants.PT_RATING_CTA_DL)
        }

        CustomRatingSubmitHandler.submit(context, payload)

        // Robolectric's package has no launcher activity, so nothing is started - and nothing crashes.
        assertNull(startedDeepLink())
        verify(exactly = 1) {
            Utils.raiseCleverTapEvent(any(), any(), PTConstants.PT_RATING_EVENT_NAME, any())
        }
    }
}
