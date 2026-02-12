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
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.clevertap.android.sdk.events.EventGroup.PUSH_NOTIFICATION_VIEWED
import com.clevertap.android.sdk.task.CTExecutorFactory
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

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
            Logger.d(
                Constants.CLEVERTAP_LOG_TAG,
                "Unable to find notification channel with id = $channelId", e
            )
            false
        }
    } else {
        areAppNotificationsEnabled()
    }

fun Context.areAppNotificationsEnabled() = try {
    NotificationManagerCompat.from(this).areNotificationsEnabled()
} catch (e: Exception) {
    Logger.d(
        Constants.CLEVERTAP_LOG_TAG,
        "Unable to query notifications enabled flag, returning true!", e
    )
    true
}

/**
 * Retrieves or creates an appropriate notification channel for displaying push notifications.
 *
 * This function implements a fallback strategy to ensure notifications can always be displayed:
 * 1. Uses the channel ID from the push payload if it exists and is registered
 * 2. Falls back to the channel ID defined in AndroidManifest if available and registered
 * 3. Creates and returns a default fallback channel if neither of the above are available
 *
 * When [hideHeadsUp] is true, the function ensures notifications use a low importance channel
 * to prevent heads-up display. Existing channels with higher importance levels are skipped
 * in favor of creating a dedicated low importance fallback channel.
 *
 * @param msgChannel The channel ID received in the push notification payload. May be null or empty.
 * @param context The application context, used to access manifest metadata and string resources.
 * @param hideHeadsUp When true, ensures the returned channel has [NotificationManager.IMPORTANCE_LOW]
 *                    to suppress heads-up notifications. Defaults to false.
 * @return The channel ID to use for the notification, or null if an unexpected error occurs
 *         during channel retrieval or creation.
 *
 * @see NotificationManager.tryGetChannel
 * @see NotificationManager.createFallbackChannel
 */
@RequiresApi(VERSION_CODES.O)
@WorkerThread
fun NotificationManager.getOrCreateChannel(
    msgChannel: String?,
    context: Context,
    hideHeadsUp: Boolean = false
): String? {
    return try {
        // Try payload channel first
        tryGetChannel(msgChannel, hideHeadsUp, "Payload")?.let { return it }

        // Try manifest channel second
        val manifestChannel = ManifestInfo.getInstance(context).devDefaultPushChannelId
        tryGetChannel(manifestChannel, hideHeadsUp, "Manifest")?.let { return it }

        // Create appropriate fallback channel
        createFallbackChannel(context, hideHeadsUp)
    } catch (e: Exception) {
        Logger.d(Constants.CLEVERTAP_LOG_TAG, "Error getting or creating notification channel", e)
        null
    }
}

/**
 * Attempts to retrieve an existing channel if it exists and meets the heads-up display requirements.
 *
 * @param channelId The channel ID to check.
 * @param hideHeadsUp Whether heads-up notifications should be suppressed.
 * @return The channel ID if valid and meets requirements, null otherwise.
 */
@RequiresApi(VERSION_CODES.O)
private fun NotificationManager.tryGetChannel(
    channelId: String?,
    hideHeadsUp: Boolean,
    channelSource: String
): String? {
    if (channelId.isNullOrEmpty()) {
        Logger.d(Constants.CLEVERTAP_LOG_TAG, "channelID from $channelSource is null or empty")
        return null
    }

    val channel = getNotificationChannel(channelId) ?: return null

    // If we need to hide heads-up but channel has high importance, skip it
    val shouldSkip = hideHeadsUp && channel.importance != NotificationManager.IMPORTANCE_LOW

    if (shouldSkip) {
        Logger.d(
            Constants.CLEVERTAP_LOG_TAG,
            "Skipping channel $channelId because heads-up should be hidden in FG but importance is ${channel.importance}"
        )
        return null
    }
    return channelId
}

/**
 * Creates and returns the appropriate fallback notification channel.
 *
 * @param context The application context.
 * @param hideHeadsUp Whether to create a low importance channel.
 * @return The ID of the created fallback channel.
 */
@RequiresApi(VERSION_CODES.O)
private fun NotificationManager.createFallbackChannel(
    context: Context,
    hideHeadsUp: Boolean
): String {
    if (hideHeadsUp) {
        return createLowImportanceFallback()
    }
    return createDefaultFallbackChannel(context)
}

/**
 * Creates a low importance channel for suppressing heads-up notifications.
 *
 * @return The ID of the created low importance channel.
 */
@RequiresApi(VERSION_CODES.O)
private fun NotificationManager.createLowImportanceFallback(): String {
    val channelId = Constants.CT_FALLBACK_NOTIFICATION_CHANNEL_ID_LOW
    if (getNotificationChannel(channelId) != null) {
        return channelId
    }

    createNotificationChannel(
        NotificationChannel(
            channelId,
            Constants.LOW_IMPORTANCE_FALLBACK_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )
    )

    Logger.d(Constants.CLEVERTAP_LOG_TAG, "Created low importance fallback channel: $channelId")
    return channelId
}

/**
 * Creates the default fallback notification channel if it doesn't exist.
 *
 * @param context The application context.
 * @return The ID of the default fallback channel.
 */
@RequiresApi(VERSION_CODES.O)
private fun NotificationManager.createDefaultFallbackChannel(context: Context): String {
    val channelId = Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_ID

    if (getNotificationChannel(channelId) == null) {
        val channelName = try {
            context.getString(R.string.ct_fcm_fallback_notification_channel_label)
        } catch (e: Exception) {
            Logger.d(
                Constants.CLEVERTAP_LOG_TAG,
                "Failed to fetch fallback channel name from resources",
                e
            )
            Constants.FCM_FALLBACK_NOTIFICATION_CHANNEL_NAME
        }

        createNotificationChannel(
            NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )

        Logger.d(Constants.CLEVERTAP_LOG_TAG, "Created default fallback channel: $channelId")
    }

    return channelId
}

/**
 * Flushes push notification impressions on the CleverTap instance in blocking fashion on a postAsyncSafely executor.
 * postAsyncSafely executor will make sure that multiple flush operations occur in sequence.

 * @param logTag is tag name to identify the task state in logs
 * @param caller The caller.
 * @param context The application context.
 */
@MainThread
fun CleverTapAPI.flushPushImpressionsOnPostAsyncSafely(
    logTag: String,
    caller: String,
    context: Context
) {
    val flushTask = CTExecutorFactory.executors(coreState.config).postAsyncSafelyTask<Void?>()

    val flushFutureResult = flushTask.submit(logTag) {
        try {
            coreState.baseEventQueueManager.flushQueueSync(
                context,
                PUSH_NOTIFICATION_VIEWED,
                caller
            )
        } catch (e: Exception) {
            Logger.d(
                logTag,
                "Failed to flush push impressions on CT instance = ${coreState.config.accountId}",
                e
            )
        }
        null
    }
    try {
        flushFutureResult.get()
    } catch (e: Exception) {
        Logger.d(logTag, "Error getting flush result for push impressions", e)
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
inline fun <reified T> JSONArray.toList(): MutableList<T> {
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
fun JSONObject.safeGetJSONArrayOrNullIfEmpty(key: String): Pair<Boolean, JSONArray?> {
    val list: JSONArray = optJSONArray(key) ?: return Pair(false, null)
    return Pair(list.length() > 0, list.takeIf { it.length() > 0 })
}

fun JSONObject.safeGetJSONObjectListOrEmpty(key: String): Pair<Boolean, List<JSONObject>> {
    val array = optJSONArray(key)
    return if (array != null) {
        Pair(true, array.toList<JSONObject>())
    } else {
        Pair(false, emptyList())
    }
}

/**
 * Safely retrieves a JSONArray from the JSONObject using the specified [key].
 *
 * @param key The key to retrieve the JSONArray.
 * @return A [Pair] indicating success and the retrieved JSONArray.
 *         The first value of the Pair is `true` if the JSONArray is not null, `false` otherwise.
 *         The second value of the Pair is the non-null JSONArray if it exists, or `null` otherwise.
 */
fun JSONObject.safeGetJSONArray(key: String): Pair<Boolean, JSONArray?> {
    val list: JSONArray = optJSONArray(key) ?: return Pair(false, null)
    return Pair(list.length() >= 0, list.takeIf { it.length() >= 0 })
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
 * Copies all key-value pairs from this JSONObject to a new JSONObject.
 */
fun JSONObject.copy(): JSONObject {
    val json = JSONObject()
    json.copyFrom(this)
    return json
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

fun String?.toJsonOrNull(): JSONObject? {
    return this?.let {
        try {
            JSONObject(it)
        } catch (e: JSONException) {
            null
        }
    }
}

@OptIn(ExperimentalContracts::class)
fun String?.isNotNullAndBlank(): Boolean {
    contract { returns(true) implies (this@isNotNullAndBlank != null) }
    return isNullOrBlank().not()
}

/**
 * Adjusts the margins of the view based on the system bar insets (such as the status bar, navigation bar,
 * or display cutout) using the provided margin adjustment logic.
 *
 * This function sets a listener on the view to handle window insets and invokes the provided `marginAdjuster`
 * block to allow custom margin adjustments. The `marginAdjuster` lambda receives the system bar insets and
 * the view's margin layout parameters, allowing the caller to modify the margins as needed.
 *
 * @param marginAdjuster A lambda function that takes two parameters:
 *  - `bars`: The insets for system bars and display cutouts, representing the space occupied by UI elements
 *     such as the status bar or navigation bar.
 *  - `mlp`: The `MarginLayoutParams` of the view, which can be modified to adjust the margins based on the insets.
 *
 * Example usage:
 * ```
 * view.applyInsetsWithMarginAdjustment { insets, layoutParams ->
 *     layoutParams.leftMargin = insets.left
 *     layoutParams.rightMargin = insets.right
 *     layoutParams.topMargin = insets.top
 *     layoutParams.bottomMargin = insets.bottom
 * }
 * ```
 */
fun View.applyInsetsWithMarginAdjustment(marginAdjuster: (insets: Insets, mlp: MarginLayoutParams) -> Unit) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars: Insets = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
        )
        v.updateLayoutParams<MarginLayoutParams> {
            marginAdjuster(bars, this)
        }
        WindowInsetsCompat.CONSUMED
    }
}

inline fun <reified T> JSONArray.partition(
    predicate: (T) -> Boolean
): Pair<JSONArray, JSONArray> {
    val first = JSONArray()
    val second = JSONArray()

    for (i in 0 until this.length()) {
        val element = this.get(i)
        if (element is T) {
            if (predicate(element)) {
                first.put(element)
            } else {
                second.put(element)
            }
        }
    }
    return first to second
}