package com.clevertap.android.pushtemplates.styles

import android.content.Context
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.clevertap.android.pushtemplates.TemplateRenderer

abstract class StyleWithActionButtons(private var renderer: TemplateRenderer) : Style(renderer) {
    override fun builderFromStyle(
        context: Context,
        extras: Bundle,
        notificationId: Int,
        nb: NotificationCompat.Builder
    ): NotificationCompat.Builder {
        val builder = super.builderFromStyle(context, extras, notificationId, nb)
        return renderer.setActionButtons(context, extras, notificationId, builder, renderer.actions)
    }
}