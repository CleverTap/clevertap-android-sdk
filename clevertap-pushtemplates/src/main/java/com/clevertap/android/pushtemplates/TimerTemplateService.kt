package com.clevertap.android.pushtemplates

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil
import com.clevertap.android.sdk.task.CTExecutorFactory

class TimerTemplateService : Service() {
    @SuppressLint("WrongConstant")
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val message = intent.extras ?: return super.onStartCommand(intent, flags, startId)
        val templateRenderer = TemplateRenderer(this@TimerTemplateService, message)
        PTLog.verbose("Running Timer Template Service")

        val cleverTapAPI = CleverTapAPI.getGlobalInstance(
            this@TimerTemplateService,
            PushNotificationUtil.getAccountIdFromNotificationBundle(message)
        )
        val config: CleverTapInstanceConfig? = cleverTapAPI?.coreState?.config

        config?.let {
            val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Void>()
            task.execute("getTimerTemplateNotificationBuilder") {
                try {
                    val notificationBuilder = cleverTapAPI.getPushNotificationOnCallerThread(
                        templateRenderer, this@TimerTemplateService, message
                    )

                    notificationBuilder?.let {
                        it.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                        val nb = it.build()
                        PTLog.verbose("Starting foreground service with notification ID: ${templateRenderer.notificationId}")
                        startForeground(templateRenderer.notificationId, nb)
                    } ?: run {
                        PTLog.verbose("NotificationBuilder is null.")
                    }
                } catch (e: Exception) {
                    PTLog.verbose("Error while creating notification: ${e.localizedMessage}")
                    e.printStackTrace()
                }
                null
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
