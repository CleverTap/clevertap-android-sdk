package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.PTLog
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.TimerTemplateData
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import com.clevertap.android.pushtemplates.content.TIMER_CONTENT_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.TimerBigContentView
import com.clevertap.android.pushtemplates.content.TimerSmallContentView

internal class TimerStyle(private val data: TimerTemplateData, renderer: TemplateRenderer) : Style(data.baseContent, renderer) {

    private val actionButtonsHandler = ActionButtonsHandler(renderer)

    override fun makeSmallContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews? {
        return if (getTimerEnd() == null)
            null
        else {
            return TimerSmallContentView(context, getTimerEnd(), renderer, data).remoteView
        }
    }

    override fun makeBigContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews? {
        return if (getTimerEnd() == null)
            null
        else {
            return TimerBigContentView(context, getTimerEnd(), renderer, data).remoteView
        }
    }

    override fun makePendingIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        return PendingIntentFactory.getPendingIntent(
            context, notificationId, extras, true,
            TIMER_CONTENT_PENDING_INTENT, data.baseContent.deepLinkList.getOrNull(0)
        )
    }

    override fun makeDismissIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        return null
    }
    
    override fun builderFromStyle(
        context: Context,
        extras: Bundle,
        notificationId: Int,
        nb: NotificationCompat.Builder
    ): NotificationCompat.Builder {
        val builder = super.builderFromStyle(context, extras, notificationId, nb)
        return actionButtonsHandler.addActionButtons(context, extras, notificationId, builder)
    }

    @Suppress("LocalVariableName")
    private fun getTimerEnd(): Int? {
        var timer_end: Int? = null
        if (data.timerThreshold != -1 && data.timerThreshold >= PTConstants.PT_TIMER_MIN_THRESHOLD) {
            timer_end =data.timerThreshold * PTConstants.ONE_SECOND + PTConstants.ONE_SECOND
        } else if (data.timerEnd >= PTConstants.PT_TIMER_MIN_THRESHOLD) {
            timer_end = data.timerEnd * PTConstants.ONE_SECOND + PTConstants.ONE_SECOND
        } else {
            PTLog.debug("Not rendering notification Timer End value lesser than threshold (10 seconds) from current time: " + PTConstants.PT_TIMER_END)
        }
        return timer_end
    }
}