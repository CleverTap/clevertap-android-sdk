package com.clevertap.android.sdk.inapp.delay

import org.json.JSONObject

/**
 * Sealed class representing the result of a delayed in-app callback
 */
sealed interface DelayedInAppResult {

    /**
     * Success state - in-app was successfully retrieved and is ready for display
     *
     * @property inAppId The unique identifier of the in-app campaign
     * @property inApp The JSONObject containing the in-app notification data
     */
    data class Success(
        val inAppId: String,
        val inApp: JSONObject
    ) : DelayedInAppResult

    /**
     * Error state - in-app retrieval failed or data is unavailable
     *
     * @property inAppId The unique identifier of the in-app campaign
     * @property reason The error reason explaining why the callback failed
     * @property throwable Optional throwable for debugging purposes
     */
    data class Error(
        val inAppId: String,
        val reason: ErrorReason,
        val throwable: Throwable? = null
    ) : DelayedInAppResult {

        /**
         * Enum representing specific error reasons
         */
        enum class ErrorReason(val message: String) {
            NOT_FOUND_IN_DB("Delayed in-app not found in database"),
            STORE_NOT_INITIALIZED("DelayedLegacyInAppStore is not initialized"),
            DB_SAVE_FAILED("Failed to save delayed in-app to database"),
            UNKNOWN("Unknown error occurred");

            override fun toString(): String = message
        }
    }

    data class Discarded(val id: String, val reason: String) : DelayedInAppResult
}

sealed interface InActionResult {
    data class ReadyToFetch(val targetId: Long, val metadata: JSONObject) : InActionResult
    data class Error(val targetId: Long, val message: String) : InActionResult
    data class Cancelled(val targetId: Long) : InActionResult
    data class Discarded(val targetId: Long, val reason: String) : InActionResult
}