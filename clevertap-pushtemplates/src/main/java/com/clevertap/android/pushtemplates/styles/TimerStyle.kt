package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import com.clevertap.android.pushtemplates.content.TIMER_CONTENT_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.TimerBigContentView
import com.clevertap.android.pushtemplates.content.TimerSmallContentView

class TimerStyle(
    private var renderer: TemplateRenderer,
    private var notificationId: Int,
    private var nb: NotificationCompat.Builder
) : Style(renderer) {

    override fun makeSmallContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews? {
        return if (getTimerEnd(true) == null)
            null
        else {
            return TimerSmallContentView(context, getTimerEnd(true), renderer).remoteView
        }
    }

    override fun makeBigContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews? {
        return if (getTimerEnd(true) == null)
            null
        else {
            return TimerBigContentView(context, getTimerEnd(true),
                getTimerEnd(false), renderer,
                notificationId,nb).remoteView
        }
    }

    override fun makePendingIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        return PendingIntentFactory.getPendingIntent(
            context, notificationId, extras, true,
            TIMER_CONTENT_PENDING_INTENT, renderer
        )
    }

    override fun makeDismissIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        return null
    }

    /**
     * Returns a timer_end value based on the the boolean. If it's true then timer_end is modified to
     * return a long value whereas if it's false then it returns timer_end as it is from the payload.
     *
     * @return int which is required to show the countdown value for chronometer/countdown thread.
     */
    @Suppress("LocalVariableName")
    private fun getTimerEnd(timerEndAsLong: Boolean): Int? {
        var timer_end: Int? = null
        if (renderer.pt_timer_threshold != -1 && renderer.pt_timer_threshold >= PTConstants.PT_TIMER_MIN_THRESHOLD) {
            timer_end = if (timerEndAsLong) renderer.pt_timer_threshold * PTConstants.ONE_SECOND + PTConstants.ONE_SECOND
            else renderer.pt_timer_threshold
        } else if (renderer.pt_timer_end >= PTConstants.PT_TIMER_MIN_THRESHOLD) {
            timer_end = if (timerEndAsLong) renderer.pt_timer_end * PTConstants.ONE_SECOND + PTConstants.ONE_SECOND
            else renderer.pt_timer_end
        } else {
            PTLog.debug("Not rendering notification Timer End value lesser than threshold (10 seconds) from current time: " + PTConstants.PT_TIMER_END)
        }
        return timer_end
    }
}