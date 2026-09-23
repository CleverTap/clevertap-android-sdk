package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import android.text.Html
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.clevertap.android.pushtemplates.IconsTemplateData
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.content.*
import com.clevertap.android.pushtemplates.content.PendingIntentFactory
import com.clevertap.android.sdk.Constants

internal class IconsStyle(private val data: IconsTemplateData, renderer: TemplateRenderer, private var extras: Bundle) : Style(data.baseContent, renderer) {

    lateinit var iconsSmallContentView: IconsContentView
    lateinit var iconsBigContentView: IconsContentView

    /**
     * The system shows the builder title in a stacked group summary. Use pt_title, else pt_msg,
     * and never the nt fallback.
     */
    override fun setNotificationBuilderBasics(
        notificationBuilder: NotificationCompat.Builder,
        contentViewSmall: RemoteViews?,
        contentViewBig: RemoteViews?,
        pt_title: String?,
        pIntent: PendingIntent?,
        dIntent: PendingIntent?
    ): NotificationCompat.Builder {
        val title = data.iconTextData.title ?: data.iconTextData.message
        return super.setNotificationBuilderBasics(
            notificationBuilder, contentViewSmall,
            contentViewBig, title.orEmpty(), pIntent, dIntent
        ).setContentTitle(title?.let { Html.fromHtml(it) })
    }

    override fun makeSmallContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews {
        iconsSmallContentView = IconsSmallContentView(context, renderer, data, extras)
        return iconsSmallContentView.remoteView
    }

    override fun makeBigContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews {
        iconsBigContentView = IconsBigContentView(context, renderer, data, extras)
        return iconsBigContentView.remoteView
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
