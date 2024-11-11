package com.clevertap.android.pushtemplates

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil
import com.clevertap.android.sdk.task.CTExecutorFactory

class TimerTemplateService : Service() {
    companion object {
        private const val TAG = "TimerTemplateService"
    }

    @SuppressLint("WrongConstant")
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val message = intent.extras ?: return super.onStartCommand(intent, flags, startId)
        val templateRenderer = TemplateRenderer(this@TimerTemplateService, message)
        Logger.v(TAG, "Running Timer Template Service")

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
                        startForeground(templateRenderer.notificationId, nb)
                        Logger.v(TAG, "Started foreground service with notification ID: ${templateRenderer.notificationId}")
                    } ?: run {
                        Logger.v(TAG, "NotificationBuilder is null.")
                    }
                } catch (e: Exception) {
                    Logger.v(TAG, "Error while creating notification: ${e.localizedMessage}")
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
