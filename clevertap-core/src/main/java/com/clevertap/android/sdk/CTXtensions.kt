@file:JvmName("CTXtensions")

package com.clevertap.android.sdk

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationManagerCompat
import com.clevertap.android.sdk.events.EventGroup.PUSH_NOTIFICATION_VIEWED
import com.clevertap.android.sdk.task.CTExecutorFactory

fun Context.isPackageAndOsTargetsAbove(apiLevel: Int) =
    VERSION.SDK_INT > apiLevel && targetSdkVersion > apiLevel

val Context.targetSdkVersion
    get() = applicationContext.applicationInfo.targetSdkVersion

fun Context.isNotificationChannelEnabled(channelId: String): Boolean =
    if (VERSION.SDK_INT >= VERSION_CODES.O) {
        areAppNotificationsEnabled() && try {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.getNotificationChannel(channelId).importance != NotificationManager.IMPORTANCE_NONE
        } catch (e: Exception) {
            Logger.d("Unable to find notification channel with id = $channelId")
            false
        }
    } else {
        areAppNotificationsEnabled()
    }

fun Context.areAppNotificationsEnabled() = try {
    NotificationManagerCompat.from(this).areNotificationsEnabled()
} catch (e: Exception) {
    Logger.d("Unable to query notifications enabled flag, returning true!")
    e.printStackTrace()
    true
}

@RequiresApi(VERSION_CODES.O)
@WorkerThread
fun NotificationManager.getOrCreateChannel(
    msgChannel: String?, context: Context
): String? {

    try {
        /**
         * if channel id is present in push payload and registered by an app then return the payload channel id
         */
        if (!msgChannel.isNullOrEmpty() && getNotificationChannel(msgChannel) != null) {
            return msgChannel
        }

        val manifestMetadata = ManifestInfo.getInstance(context)
        val manifestChannel = manifestMetadata.devDefaultPushChannelId

        /**
         * if channel id is present in manifest and registered by an app then return the manifest channel id
         */
        if (!manifestChannel.isNullOrEmpty() && getNotificationChannel(manifestChannel) != null) {
            return manifestChannel
        }

        if (manifestChannel.isNullOrEmpty()) {
            Logger.d(
                Constants.CLEVERTAP_LOG_TAG,
                "Missing Default CleverTap Notification Channel metadata in AndroidManifest."
            )
        } else {
            Logger.d(
                Constants.CLEVERTAP_LOG_TAG,
                "Notification Channel set in AndroidManifest.xml has not been created by the app."
            )
        }

        /**
         * create default channel
         */
        if (getNotificationChannel(Constants.CLEVERTAP_RESERVE_CHANNEL_ID) == null) {

            createNotificationChannel(
                NotificationChannel(
                    Constants.CLEVERTAP_RESERVE_CHANNEL_ID,
                    Constants.CLEVERTAP_RESERVE_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).also {
                    Logger.d(Constants.CLEVERTAP_LOG_TAG, "created default channel: $it")
                }
            )
        }

        return Constants.CLEVERTAP_RESERVE_CHANNEL_ID
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

/**
 * Flushes push notification impressions on the CleverTap instance in blocking fashion on a postAsyncSafely executor.
 * postAsyncSafely executor will make sure that multiple flush operations occur in sequence.

 * @param logTag is tag name to identify the task state in logs
 * @param caller The caller.
 * @param context The application context.
 */
fun CleverTapAPI.flushPushImpressionsOnPostAsyncSafely(logTag: String, caller: String, context: Context) {
    val flushTask = CTExecutorFactory.executors(coreState.config).postAsyncSafelyTask<Void?>()

    val flushFutureResult = flushTask.submit(logTag) {
        try {
            coreState.baseEventQueueManager.flushQueueSync(context, PUSH_NOTIFICATION_VIEWED, caller)
        } catch (e: Exception) {
            Logger.d(logTag, "failed to flush push impressions on ct instance = " + coreState.config.accountId)
        }
        null
    }

    try {
        flushFutureResult.get()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
