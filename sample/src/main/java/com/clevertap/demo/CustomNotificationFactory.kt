package com.clevertap.demo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.clevertap.android.sdk.pushnotification.ICleverTapNotificationFactory

/**
 * Custom notification factory demonstrating how to render CleverTap Live Activity
 * (live update) pushes with full control over the notification content.
 *
 * The client builds the notification channel and the notification itself. The SDK owns:
 * - the notification ID (derived from the backend `cleverTapActivityId`, so every update lands
 *   on the same notification and updates in place),
 * - dismissal tracking (delete intent), and
 * - the "Live Activity" lifecycle events (Started / Updated / Ended / Dismissed).
 *
 * Because the SDK derives the ID, returning `notificationId = 0` here is fine — it is only used
 * as a fallback when the push carries no activity id.
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
            .setOnlyAlertOnce(true) // updates should not re-alert on every push
            .setAutoCancel(true)

        // notificationId is a fallback only; the SDK derives the real id from cleverTapActivityId.
        return ICleverTapNotificationFactory.NotificationResult(nb.build(), 0)
    }
}
