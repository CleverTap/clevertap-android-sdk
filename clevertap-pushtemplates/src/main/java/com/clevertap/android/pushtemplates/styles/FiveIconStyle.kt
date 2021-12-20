package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.content.FIVE_ICON_CONTENT_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.FiveIconBigContentView
import com.clevertap.android.pushtemplates.content.FiveIconSmallContentView
import com.clevertap.android.pushtemplates.content.PendingIntentFactory

class FiveIconStyle(private var renderer: TemplateRenderer, private var extras: Bundle) : Style(renderer) {

    override fun makeSmallContentView(context: Context, renderer: TemplateRenderer): RemoteViews {
        return FiveIconSmallContentView(context, renderer, extras).remoteView
    }

    override fun makeBigContentView(context: Context, renderer: TemplateRenderer): RemoteViews {
        return FiveIconBigContentView(context, renderer, extras).remoteView
    }

    override fun makePendingIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        return PendingIntentFactory.getPendingIntent(
            context, notificationId, extras, true,
            FIVE_ICON_CONTENT_PENDING_INTENT, renderer
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