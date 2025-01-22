package com.clevertap.android.pushtemplates

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.BigTextStyle
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.getOrCreateChannel
import com.clevertap.android.sdk.pushnotification.PushNotificationUtil
import com.clevertap.android.sdk.task.CTExecutorFactory
import kotlin.random.Random

class TimerTemplateService : Service() {
    @SuppressLint("WrongConstant")
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // todo refactor this function to make it more readable
        val message = intent.extras ?: return START_NOT_STICKY
        val templateRenderer = TemplateRenderer(this@TimerTemplateService, message)
        PTLog.verbose("Running TimerTemplateService")

        val cleverTapAPI = CleverTapAPI.getGlobalInstance(
            this@TimerTemplateService,
            PushNotificationUtil.getAccountIdFromNotificationBundle(message)
        )
        val config: CleverTapInstanceConfig? = cleverTapAPI?.coreState?.config
        val smallIcon: Int = cleverTapAPI?.coreState?.pushProviders!!.getSmallIcon(applicationContext)
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val msgChannel = message.getString(Constants.WZRK_CHANNEL_ID, "")
        val notificationChannel = if (Build.VERSION.SDK_INT >= VERSION_CODES.O) {
            notificationManager.getOrCreateChannel(msgChannel, applicationContext) ?: ""
        }
        else {
            ""
        }

        PTLog.verbose("Starting foreground service with placeholder notification")
        startForeground(
            Random.nextInt(1, Int.MAX_VALUE),
            NotificationCompat.Builder(this@TimerTemplateService, notificationChannel)
                .setContentTitle(templateRenderer.pt_title)
                .setContentText(templateRenderer.pt_msg)
                .setSmallIcon(smallIcon)
                .setStyle(BigTextStyle())
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
                .build())

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
                        cleverTapAPI.coreState?.pushProviders?.storePushNotification(message)
                        cleverTapAPI.coreState.pushProviders?.processPushNotificationViewedEvent(message)
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
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        PTLog.verbose("TimerTemplateService Stopped")
        super.onDestroy()
    }
}