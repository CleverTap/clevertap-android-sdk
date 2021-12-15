package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.content.AUTO_CAROUSEL_CONTENT_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.AutoCarouselContentView
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import com.clevertap.android.pushtemplates.content.SmallContentView

class AutoCarouselStyle(private var renderer: TemplateRenderer) : Style(renderer) {

    override fun makeSmallContentView(context: Context, renderer: TemplateRenderer): RemoteViews {
        return SmallContentView(context, renderer).remoteView
    }

    override fun makeBigContentView(context: Context, renderer: TemplateRenderer): RemoteViews {
        return AutoCarouselContentView(context, renderer).remoteView
    }

    override fun makePendingIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        return PendingIntentFactory.getPendingIntent(
            context, notificationId, extras, true,
            AUTO_CAROUSEL_CONTENT_PENDING_INTENT, renderer
        )
    }

    override fun makeDismissIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        return null
    }
}