package com.clevertap.android.pushtemplates.styles

import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.clevertap.android.pushtemplates.BaseContent
import com.clevertap.android.pushtemplates.PTConstants
import com.clevertap.android.pushtemplates.TemplateRenderer

internal abstract class Style(private val data: BaseContent, private val renderer: TemplateRenderer) {

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notificationBuilder.setSubText(data.textData.subtitle)
        }

        data.notificationBehavior.dismissAfter?.let { timeout ->
            notificationBuilder.setTimeoutAfter(timeout)
        }

        return notificationBuilder.setSmallIcon(renderer.smallIcon)
            .setContentTitle(Html.fromHtml(pt_title))
            .setContentIntent(pIntent)
            .setVibrate(longArrayOf(0L))
            .setWhen(System.currentTimeMillis())
            .setColor(Color.parseColor(data.colorData.smallIconColor?: PTConstants.PT_META_CLR_DEFAULTS))
            .setAutoCancel(true)
            .setOngoing(data.notificationBehavior.isSticky)
            .setStyle(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    NotificationCompat.DecoratedCustomViewStyle()
                } else {
                    null
                }
            )
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
            data.textData.title, makePendingIntent(context, extras, notificationId),
            makeDismissIntent(context, extras, notificationId)
        )
    }
}