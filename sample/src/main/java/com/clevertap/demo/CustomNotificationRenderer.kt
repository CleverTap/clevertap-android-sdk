package com.clevertap.demo

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.pushnotification.INotificationRenderer

/**
 * Custom notification renderer that demonstrates how to customize push notifications.
 * This renderer adds custom styling, colors, and behavior to notifications.
 */
class CustomNotificationRenderer : INotificationRenderer {

    private var smallIcon: Int = 0

    override fun setSmallIcon(smallIcon: Int, context: Context) {
        this.smallIcon = smallIcon
    }

    override fun getCollapseKey(extras: Bundle?): Any? {
        return extras?.getString("wzrk_ck")
    }

    override fun getMessage(extras: Bundle?): String? {
        return extras?.getString("nm") ?: extras?.getString("nt")
    }

    override fun getTitle(extras: Bundle?, context: Context): String? {
        return extras?.getString("nt") ?: context.applicationInfo.loadLabel(context.packageManager).toString()
    }

    override fun getActionButtonIconKey(): String {
        return "ico"
    }

    override fun renderNotification(
        extras: Bundle?,
        context: Context,
        nb: NotificationCompat.Builder,
        config: CleverTapInstanceConfig,
        notificationId: Int
    ): NotificationCompat.Builder? {
        
        if (extras == null) return null

        // Get basic notification info
        val title = "Custom"
        val message = "This is Custom"

        // Set title and message
        title?.let { nb.setContentTitle(it) }
        message?.let { nb.setContentText(it) }

        // Apply custom styling
        nb.setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(message)
                .setBigContentTitle(title)
        )

        // Set custom colors - Purple accent for demonstration
        nb.setColor(Color.parseColor("#9C27B0"))
        
        // Set small icon
        if (smallIcon != 0) {
            nb.setSmallIcon(smallIcon)
        }

        // Set notification priority
        nb.priority = NotificationCompat.PRIORITY_HIGH

        // Add custom vibration pattern (short vibrations)
        nb.setVibrate(longArrayOf(0, 200, 100, 200))

        // Set LED color to purple
        nb.setLights(Color.parseColor("#9C27B0"), 1000, 1000)

        // Auto cancel when clicked
        nb.setAutoCancel(true)

        return nb
    }
}
