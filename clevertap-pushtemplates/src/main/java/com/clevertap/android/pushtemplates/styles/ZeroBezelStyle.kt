package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.ZeroBezelTemplateData
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import com.clevertap.android.pushtemplates.content.ZERO_BEZEL_CONTENT_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.ZeroBezelBigContentView
import com.clevertap.android.pushtemplates.content.ZeroBezelSmallContentView

internal class ZeroBezelStyle(private val data: ZeroBezelTemplateData, renderer: TemplateRenderer) : Style(data.baseContent, renderer) {

    private val actionButtonsHandler = ActionButtonsHandler(renderer)

    override fun makeSmallContentRemoteView(
        context: Context,
        renderer: TemplateRenderer
    ): RemoteViews {
        return ZeroBezelSmallContentView(context,renderer, data).remoteView
    }

    override fun makeBigContentRemoteView(
        context: Context,
        renderer: TemplateRenderer
    ): RemoteViews {
        return ZeroBezelBigContentView(context, renderer, data).remoteView
    }

    override fun makePendingIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        return PendingIntentFactory.getPendingIntent(
            context, notificationId, extras, true,
            ZERO_BEZEL_CONTENT_PENDING_INTENT, data.baseContent.deepLinkList.getOrNull(0)
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
}