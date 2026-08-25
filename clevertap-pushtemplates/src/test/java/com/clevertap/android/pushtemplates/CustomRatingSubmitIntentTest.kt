package com.clevertap.android.pushtemplates

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Guards the Android 12 trampoline rule for the pt_custom_rating template (R-34).
 *
 * A notification tap handled by a broadcast receiver may not start an activity from Android 12
 * onwards, and this template family has shipped that bug twice before. Submitting a rating has to
 * open the campaign's destination, so its tap must go straight to an activity; selecting a position
 * only redraws the notification, so its tap must stay a broadcast.
 */
@RunWith(RobolectricTestRunner::class)
class CustomRatingSubmitIntentTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()
    private val notificationId = 42

    private fun extras() = Bundle().apply {
        putString(PTConstants.PT_ID, "pt_custom_rating")
        putString(PTConstants.PT_RATING_CTA_DL, "myapp://feedback")
    }

    private fun submitIntent(selectedPosition: Int): PendingIntent =
        PendingIntentFactory.getCustomRatingSubmitIntent(
            context, notificationId, extras(), selectedPosition, null
        )

    @Test
    fun `submitting a selected rating goes straight to an activity, never through the receiver`() {
        val shadow = shadowOf(submitIntent(selectedPosition = 3))

        assertTrue(shadow.isActivityIntent)
        assertEquals(
            PTRatingSubmitActivity::class.java.name,
            shadow.savedIntent.component?.className
        )
    }

    @Test
    fun `the submit intent carries the selected position it has to report`() {
        val saved = shadowOf(submitIntent(selectedPosition = 4)).savedIntent

        assertEquals(4, saved.getIntExtra(PTConstants.PT_RATING_SELECTED_POSITION, 0))
        assertEquals(notificationId, saved.getIntExtra(PTConstants.PT_NOTIF_ID, 0))
        assertTrue(saved.getBooleanExtra(PTConstants.PT_RATING_SUBMIT, false))
    }

    @Test
    fun `submitting with no selection stays a broadcast, so the tap is swallowed and nothing opens`() {
        val shadow = shadowOf(submitIntent(selectedPosition = 0))

        assertTrue(shadow.isBroadcastIntent)
        assertEquals(
            PushTemplateReceiver::class.java.name,
            shadow.savedIntent.component?.className
        )
    }

    @Test
    fun `selecting a position stays a broadcast, because it only redraws the notification`() {
        val shadow = shadowOf(
            PendingIntentFactory.getCustomRatingPositionIntent(
                context, notificationId, extras(), 2, null
            )
        )

        assertTrue(shadow.isBroadcastIntent)
        assertEquals(
            PushTemplateReceiver::class.java.name,
            shadow.savedIntent.component?.className
        )
        assertEquals(2, shadow.savedIntent.getIntExtra(PTConstants.PT_RATING_SELECTED_POSITION, 0))
    }

    @Test
    fun `each position and the submit button own a distinct request code`() {
        val requestCodes = (1..PTConstants.PT_RATING_COUNT_MAX).map { position ->
            shadowOf(
                PendingIntentFactory.getCustomRatingPositionIntent(
                    context, notificationId, extras(), position, null
                )
            ).requestCode
        } + shadowOf(submitIntent(selectedPosition = 1)).requestCode

        assertEquals(requestCodes.size, requestCodes.distinct().size)
    }

    @Test
    fun `two notifications do not share the submit request code`() {
        val first = shadowOf(submitIntent(selectedPosition = 1)).requestCode
        val second = shadowOf(
            PendingIntentFactory.getCustomRatingSubmitIntent(
                context, notificationId + 1, extras(), 1, null
            )
        ).requestCode

        assertNotEquals(first, second)
    }
}
