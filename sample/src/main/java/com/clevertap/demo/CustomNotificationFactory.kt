package com.clevertap.demo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.clevertap.android.sdk.pushnotification.ICleverTapNotificationFactory
import java.util.Random

/**
 * Custom notification factory that demonstrates how to take full control
 * of push notification creation. The client is responsible for creating
 * the notification channel, building the notification, and choosing the ID.
 */
class CustomNotificationFactory : ICleverTapNotificationFactory {

    override fun onCreateNotification(
        context: Context,
        extras: Bundle
    ): ICleverTapNotificationFactory.NotificationResult? {

        val channelId = "custom_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Custom Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        val title = extras.getString("nt") ?: context.applicationInfo.loadLabel(context.packageManager).toString()
        val message = extras.getString("nm") ?: return null

        val deleteIntent = createDeleteIntent(context, extras)

        val notificationId = extras.getString("wzrk_ck")?.hashCode() ?: (Math.random() * 100).toInt()

        val nb = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(message)
                    .setBigContentTitle(title)
            )
            .setColor(Color.parseColor("#9C27B0"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .setLights(Color.parseColor("#9C27B0"), 1000, 1000)
            .setAutoCancel(true)
            .setDeleteIntent(deleteIntent)

        return ICleverTapNotificationFactory.NotificationResult(nb.build(), notificationId)
    }

    private fun createDeleteIntent(context: Context, extras: Bundle): PendingIntent {
        val dismissIntent = Intent(context, NotificationDismissedReceiver::class.java)
        dismissIntent.putExtras(extras)

        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }

        return PendingIntent.getBroadcast(context, Random().nextInt(), dismissIntent, flags)
    }
}
