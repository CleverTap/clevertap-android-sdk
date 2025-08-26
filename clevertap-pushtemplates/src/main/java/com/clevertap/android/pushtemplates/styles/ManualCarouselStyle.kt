package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.clevertap.android.pushtemplates.ManualCarouselTemplateData
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.content.MANUAL_CAROUSEL_CONTENT_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.MANUAL_CAROUSEL_DISMISS_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.ManualCarouselContentView
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import com.clevertap.android.pushtemplates.content.SmallContentView
import com.clevertap.android.sdk.Constants

internal class ManualCarouselStyle(
    private val data: ManualCarouselTemplateData,
    private var renderer: TemplateRenderer,
    private var extras: Bundle
) : Style(data.carouselData.baseContent, renderer) {

    private val actionButtonsHandler = ActionButtonsHandler(renderer)

    override fun makeSmallContentRemoteView(
        context: Context,
        renderer: TemplateRenderer
    ): RemoteViews {
        return SmallContentView(context, renderer, data.carouselData.baseContent).remoteView
    }

    override fun makeBigContentRemoteView(
        context: Context,
        renderer: TemplateRenderer
    ): RemoteViews {
        return ManualCarouselContentView(context, renderer, data, extras).remoteView
    }

    override fun makePendingIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        val extrasFrom = extras.getString(Constants.EXTRAS_FROM)
        return if (extrasFrom == null || extrasFrom != "PTReceiver") {
            PendingIntentFactory.getPendingIntent(
                context, notificationId, extras, true,
                MANUAL_CAROUSEL_CONTENT_PENDING_INTENT, renderer
            )
        } else {
            PendingIntentFactory.getPendingIntent(
                context, notificationId, extras, true,
                MANUAL_CAROUSEL_CONTENT_PENDING_INTENT, null
            )
        }
    }

    override fun makeDismissIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        return PendingIntentFactory.getPendingIntent(
            context, notificationId, extras, false,
            MANUAL_CAROUSEL_DISMISS_PENDING_INTENT, renderer
        )
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