package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.content.*
import com.clevertap.android.pushtemplates.content.PendingIntentFactory

class FiveIconStyle(private var renderer: TemplateRenderer, private var extras: Bundle) : Style(renderer) {

    lateinit var fiveIconSmallContentView: ContentView
    lateinit var fiveIconBigContentView: ContentView

    override fun makeSmallContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews {
        fiveIconSmallContentView = FiveIconSmallContentView(context, renderer, extras)
        return fiveIconSmallContentView.remoteView
    }

    override fun makeBigContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews {
        fiveIconBigContentView = FiveIconBigContentView(context, renderer, extras)
        return fiveIconBigContentView.remoteView
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