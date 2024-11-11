package com.clevertap.android.pushtemplates

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil
import com.clevertap.android.sdk.task.CTExecutorFactory


class TimerTemplateService : Service() {

    @SuppressLint("WrongConstant")
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val message = intent.extras!!
        val templateRenderer = TemplateRenderer(this@TimerTemplateService, message)

        val cleverTapAPI = CleverTapAPI.getGlobalInstance(
            this@TimerTemplateService,
            PushNotificationUtil.getAccountIdFromNotificationBundle(message)
        )
        val config: CleverTapInstanceConfig? = cleverTapAPI?.coreState?.config

        config.let {
            val task = CTExecutorFactory.executors(config).postAsyncSafelyTask<Void>()
            task.execute("TimerTemplateService") {
                try {

                    val notificationBuilder = cleverTapAPI?.getPushNotificationOnCallerThread(
                        templateRenderer, this@TimerTemplateService, message
                    )

                    notificationBuilder?.let {
                        it.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                        val nb = it.build()
                        startForeground(templateRenderer.notificationId, nb)
                    }
                } catch (e: Exception) {
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
