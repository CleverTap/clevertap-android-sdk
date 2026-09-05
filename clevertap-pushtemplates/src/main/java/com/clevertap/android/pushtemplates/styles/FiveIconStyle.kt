package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.clevertap.android.pushtemplates.FiveIconsTemplateData
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.content.*
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import com.clevertap.android.sdk.Constants

internal class FiveIconStyle(private val data: FiveIconsTemplateData, renderer: TemplateRenderer, private var extras: Bundle) : Style(data.baseContent, renderer) {

    lateinit var fiveIconSmallContentView: ContentView
    lateinit var fiveIconBigContentView: ContentView

    /**
     * Passes pt_title instead of baseContent's title, which falls back to nt. An icon-only
     * five icons notification must not surface the base title once the system stacks it into
     * a group summary. baseContent keeps the fallback for the basic template fallback path.
     */
    override fun setNotificationBuilderBasics(
        notificationBuilder: NotificationCompat.Builder,
        contentViewSmall: RemoteViews?,
        contentViewBig: RemoteViews?,
        pt_title: String?,
        pIntent: PendingIntent?,
        dIntent: PendingIntent?
    ): NotificationCompat.Builder {
        return super.setNotificationBuilderBasics(
            notificationBuilder, contentViewSmall,
            contentViewBig, data.iconTextData.title, pIntent, dIntent
        )
    }

    override fun makeSmallContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews {
        fiveIconSmallContentView = FiveIconSmallContentView(context, renderer, data, extras)
        return fiveIconSmallContentView.remoteView
    }

    override fun makeBigContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews {
        fiveIconBigContentView = FiveIconBigContentView(context, renderer, data, extras)
        return fiveIconBigContentView.remoteView
    }

    override fun makePendingIntent(
        context: Context,
        extras: Bundle,
        notificationId: Int
    ): PendingIntent? {
        return PendingIntentFactory.getPendingIntent(
            context, notificationId, extras, true,
            FIVE_ICON_CONTENT_PENDING_INTENT, extras.getString(Constants.DEEP_LINK_KEY)
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
