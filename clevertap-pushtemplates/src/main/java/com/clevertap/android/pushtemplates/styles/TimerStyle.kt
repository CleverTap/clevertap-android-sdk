package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import com.clevertap.android.pushtemplates.content.TIMER_CONTENT_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.TimerBigContentView
import com.clevertap.android.pushtemplates.content.TimerSmallContentView

class TimerStyle(private var renderer: TemplateRenderer, private var extras: Bundle) : Style(renderer) {

    override fun makeSmallContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews? {
        return if (getTimerEnd() == null)
            null
        else {
            return TimerSmallContentView(context, getTimerEnd(), renderer).remoteView
        }
    }

    override fun makeBigContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews? {
        return if (getTimerEnd() == null)
            null
        else {
            return TimerBigContentView(context, getTimerEnd(), renderer).remoteView
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

    @Suppress("LocalVariableName")
    private fun getTimerEnd(): Int? {
        var timer_end: Int? = null
        if (renderer.pt_timer_threshold != -1 && renderer.pt_timer_threshold >= PTConstants.PT_TIMER_MIN_THRESHOLD) {
            timer_end = renderer.pt_timer_threshold * PTConstants.ONE_SECOND + PTConstants.ONE_SECOND
        } else if (renderer.pt_timer_end >= PTConstants.PT_TIMER_MIN_THRESHOLD) {
            timer_end = renderer.pt_timer_end * PTConstants.ONE_SECOND + PTConstants.ONE_SECOND
        } else {
            PTLog.debug("Not rendering notification Timer End value lesser than threshold (10 seconds) from current time: " + PTConstants.PT_TIMER_END)
        }
        return timer_end
    }
}