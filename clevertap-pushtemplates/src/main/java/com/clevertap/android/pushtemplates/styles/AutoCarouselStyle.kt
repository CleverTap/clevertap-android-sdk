package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.clevertap.android.pushtemplates.AutoCarouselTemplateData
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.content.AUTO_CAROUSEL_CONTENT_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.AutoCarouselContentView
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import com.clevertap.android.pushtemplates.content.SmallContentView

internal class AutoCarouselStyle(private val data: AutoCarouselTemplateData, private var renderer: TemplateRenderer) : Style(data.carouselData.baseContent, renderer) {

    private val actionButtonsHandler = ActionButtonsHandler(renderer)

    override fun makeSmallContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews {
        return SmallContentView(context, renderer, data.carouselData.baseContent).remoteView
    }

    override fun makeBigContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews {
        return AutoCarouselContentView(context, renderer, data).remoteView
    }

    override fun makePendingIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        return PendingIntentFactory.getPendingIntent(
            context, notificationId, extras, true,
            AUTO_CAROUSEL_CONTENT_PENDING_INTENT, data.carouselData.baseContent.deepLinkList.getOrNull(0)
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