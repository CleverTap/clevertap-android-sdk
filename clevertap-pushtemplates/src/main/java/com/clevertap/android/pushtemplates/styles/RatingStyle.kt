package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import com.clevertap.android.pushtemplates.RatingTemplateData
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import com.clevertap.android.pushtemplates.content.RATING_CONTENT_PENDING_INTENT
import com.clevertap.android.pushtemplates.content.RatingContentView
import com.clevertap.android.pushtemplates.content.SmallContentView

internal class RatingStyle(private val data: RatingTemplateData, renderer: TemplateRenderer, private var extras: Bundle) : Style(data.baseContent, renderer) {

    override fun makeSmallContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews {
        return SmallContentView(context, renderer, data.baseContent).remoteView
    }

    override fun makeBigContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews {
        return RatingContentView(context, renderer, data, extras).remoteView
    }

    override fun makePendingIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        return PendingIntentFactory.getPendingIntent(
            context, notificationId, extras, false,
            RATING_CONTENT_PENDING_INTENT, data.defaultDeepLink
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