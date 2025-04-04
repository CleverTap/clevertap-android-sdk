package com.clevertap.android.pushtemplates.styles

import android.content.Context
import android.os.Build
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || this is InputBoxStyle) {
            // Make sure the notification in collapsed state doesn't take up action buttons
            // InputBox Template use Android CTAs for all API levels
            renderer.attachActionButtons(builder, renderer.actionButtons)
        }
        return builder
    }
}