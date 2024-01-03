@file:JvmName("CTXtensions")

package com.clevertap.android.sdk

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.SharedPreferences
import android.location.Location
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationManagerCompat
import com.clevertap.android.sdk.events.EventGroup.PUSH_NOTIFICATION_VIEWED
import com.clevertap.android.sdk.task.CTExecutorFactory
import org.json.JSONArray
import org.json.JSONObject

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

/**
 * Retrieves or creates the notification channel based on the given channel ID.
 * If the given channel ID is not registered, it falls back to the manifest channel ID.
 * If the manifest channel ID is not registered or not available, it creates and returns the default channel ID.
 *
 * @param msgChannel The channel ID received in the push payload.
 * @param context The context of the application.
 * @return The channel ID of the notification channel to be used.
 */
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
         * create fallback channel
         */
        if (getNotificationChannel(Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID) == null) {

            val defaultChannelName = try {
                  context.getString(R.string.ct_fcm_fallback_notification_channel_label)
            } catch (e: Exception) {
                Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_NAME
            }

            createNotificationChannel(
                NotificationChannel(
                    Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID,
                    defaultChannelName,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).also {
                    Logger.d(Constants.CLEVERTAP_LOG_TAG, "created default channel: $it")
                }
            )
        }

        return Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID
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

/**
 * Checks whether the given index is a valid index within the JSONArray.
 *
 * @param index The index to be checked.
 * @return `true` if the JSONArray is null or the index is out of bounds, `false` otherwise.
 */
fun JSONArray?.isInvalidIndex(index: Int): Boolean {
    return this == null || index < 0 || index >= this.length()
}

/**
 * Extension function to check if a SharedPreferences file has data.
 * @return `true` if the SharedPreferences file has data, `false` otherwise.
 */
fun SharedPreferences.hasData(): Boolean {
    return all.isNotEmpty()
}

/**
 * Returns the original JSONArray if not null, or an empty JSONArray if the original is null.
 *
 * @return The original JSONArray or an empty JSONArray.
 */
fun JSONArray?.orEmptyArray(): JSONArray {
    return this ?: JSONArray()
}

/**
 * Converts a JSONArray to a List of elements of type [T].
 *
 * @return A List of elements of type [T] extracted from the JSONArray.
 * @reified T The expected type of elements in the list.
 */
inline fun <reified T> JSONArray.toList(): List<T> {
    val list = mutableListOf<T>()
    for (index in 0 until length()) {
        val element = get(index)
        if (element is T) {
            list.add(element)
        }
    }
    return list
}

/**
 * Iterates over the elements of the JSONArray of type [T].
 *
 * @param foreach Lambda function to be executed for each element of type [T].
 * @reified T The expected type of elements in the JSONArray.
 */
inline fun <reified T> JSONArray.iterator(foreach: (element: T) -> Unit) {
    for (index in 0 until length()) {
        val element = get(index)
        if (element is T) {
            foreach(element)
        }
    }
}

/**
 * Safely retrieves a JSONArray from the JSONObject using the specified [key].
 *
 * @param key The key to retrieve the JSONArray.
 * @return A [Pair] indicating success and the retrieved JSONArray.
 *         The first value of the Pair is `true` if the JSONArray is not null and not empty, `false` otherwise.
 *         The second value of the Pair is the non-empty JSONArray if it exists, or `null` otherwise.
 */
fun JSONObject.safeGetJSONArray(key: String): Pair<Boolean, JSONArray?> {
    val list: JSONArray = optJSONArray(key) ?: return Pair(false, null)
    return Pair(list.length() > 0, list.takeIf { it.length() > 0 })
}

/**
 * Copies all key-value pairs from the specified [other] JSONObject to this JSONObject.
 *
 * @param other The JSONObject to copy key-value pairs from.
 */
fun JSONObject.copyFrom(other: JSONObject) {
    for (key in other.keys()) {
        this.put(key, other.opt(key))
    }
}

/**
 * Checks if the JSONObject is not null and has at least one key-value pair.
 *
 * @return `true` if the JSONObject is not null and not empty, `false` otherwise.
 */
fun JSONObject?.isNotNullAndEmpty() = this != null && this.length() > 0

/**
 * Concatenates this String with another String if it is not null.
 *
 * This extension function checks if both the receiver String (denoted by 'this') and the [other] String are not null.
 * If both Strings are not null, they are concatenated using the specified [separator]. If either String is null,
 * it returns the non-null String, or null if both are null.
 *
 * @param other The String to concatenate with the receiver String.
 * @param separator The separator between the two Strings (default is an empty string).
 * @return The concatenated String or null if both Strings are null.
 *
 * Example Usage:
 * ```
 * val result = "Hello".concatIfNotNull("World", ", ")
 * // Result: "Hello, World"
 *
 * val nullResult = null.concatIfNotNull("World")
 * // Result: World
 * ```
 */
fun String?.concatIfNotNull(other: String?, separator: String = ""): String? {
    return if (this != null && other != null) {
        this + separator + other
    } else {
        this ?: other
    }
}

/**
 * Checks if the Location is valid, i.e., latitude is in the range [-90.0, 90.0] and
 * longitude is in the range [-180.0, 180.0].
 *
 * @return `true` if the Location is valid, `false` otherwise.
 */
fun Location.isValid(): Boolean {
    return this.latitude in -90.0..90.0 && this.longitude in -180.0..180.0
}