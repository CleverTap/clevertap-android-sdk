package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Html
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.clevertap.android.pushtemplates.TemplateRenderer

abstract class Style(private var renderer: TemplateRenderer) {

    protected open fun setNotificationBuilderBasics(
        notificationBuilder: NotificationCompat.Builder,
        contentViewSmall: RemoteViews?,
        contentViewBig: RemoteViews?,
        pt_title: String?,
        pIntent: PendingIntent?,
        dIntent: PendingIntent? = null
    ): NotificationCompat.Builder {
        if (dIntent != null) {
            notificationBuilder.setDeleteIntent(dIntent)
        }
        if (contentViewSmall != null) {
            notificationBuilder.setCustomContentView(contentViewSmall)
        }
        if (contentViewBig != null) {
            notificationBuilder.setCustomBigContentView(contentViewBig)
        }
        return notificationBuilder.setSmallIcon(renderer.smallIcon)
            .setContentTitle(Html.fromHtml(pt_title))
            .setContentIntent(pIntent)
            .setVibrate(longArrayOf(0L))
            .setWhen(System.currentTimeMillis())
            .setColor(Color.parseColor(renderer.pt_small_icon_clr ?: "#FFFFFF"))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
    }

    protected abstract fun makeSmallContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews?

    protected abstract fun makeBigContentRemoteView(context: Context, renderer: TemplateRenderer): RemoteViews?

    protected abstract fun makePendingIntent(context: Context, extras: Bundle, notificationId: Int): PendingIntent?

    protected abstract fun makeDismissIntent(context: Context, extras: Bundle, notificationId: Int): PendingIntent?

    open fun builderFromStyle(
        context: Context, extras: Bundle, notificationId: Int,
        nb: NotificationCompat.Builder
    ): NotificationCompat.Builder {
        return setNotificationBuilderBasics(
            nb, makeSmallContentRemoteView(context, renderer), makeBigContentRemoteView(context, renderer),
            renderer.pt_title, makePendingIntent(context, extras, notificationId),
            makeDismissIntent(context, extras, notificationId)
        )
    }
}