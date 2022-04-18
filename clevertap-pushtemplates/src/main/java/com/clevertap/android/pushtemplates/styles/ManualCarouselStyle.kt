package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.content.MANUAL_CAROUSEL_CONTENT_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.MANUAL_CAROUSEL_DISMISS_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.ManualCarouselContentView
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import com.clevertap.android.pushtemplates.content.SmallContentView
import com.clevertap.android.sdk.Constants

class ManualCarouselStyle(private var renderer: TemplateRenderer, private var extras: Bundle) : Style(renderer) {

    override fun makeSmallContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews {
        return SmallContentView(context, renderer).remoteView
    }

    override fun makeBigContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews {
        return ManualCarouselContentView(context, renderer, extras).remoteView
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
}