package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.content.BASIC_CONTENT_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.BigImageContentView
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import com.clevertap.android.pushtemplates.content.SmallContentView

internal class BasicStyle(private var renderer: TemplateRenderer) : Style(renderer) {

    private val actionButtonsHandler = ActionButtonsHandler(renderer)

    override fun makeSmallContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews {
        return SmallContentView(context, renderer).remoteView
    }

    override fun makeBigContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews {
        return BigImageContentView(context, renderer).remoteView
    }

    override fun makePendingIntent(context: Context, extras: Bundle, notificationId: Int): PendingIntent? {
        return PendingIntentFactory.getPendingIntent(
            context, notificationId, extras, true,
            BASIC_CONTENT_PENDING_INTENT, renderer
        )
    }

    override fun makeDismissIntent(context: Context, extras: Bundle, notificationId: Int): PendingIntent? {
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