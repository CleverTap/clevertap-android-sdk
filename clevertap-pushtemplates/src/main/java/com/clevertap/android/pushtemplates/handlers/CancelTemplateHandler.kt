package com.clevertap.android.pushtemplates.handlers

import android.app.NotificationManager
import android.content.Context
import com.clevertap.android.pushtemplates.CancelTemplateData
import com.clevertap.android.pushtemplates.isNotNullAndEmpty

internal object CancelTemplateHandler {
    internal fun renderCancelNotification(context: Context, templateData: CancelTemplateData) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (templateData.cancelNotificationId.isNotNullAndEmpty()) {
            val notificationId = templateData.cancelNotificationId.toInt()
            notificationManager.cancel(notificationId)
        } else {
            if (templateData.cancelNotificationIds.isNotEmpty()) {
                for (ids in templateData.cancelNotificationIds) {
                    notificationManager.cancel(ids)
                }
            }
        }
    }
}