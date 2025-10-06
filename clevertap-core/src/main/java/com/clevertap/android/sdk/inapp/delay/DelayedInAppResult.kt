package com.clevertap.android.sdk.inapp.delay

import org.json.JSONObject

/**
 * Sealed class representing the result of a delayed in-app callback
 */
sealed class DelayedInAppResult {

    /**
     * Success state - in-app was successfully retrieved and is ready for display
     *
     * @property inApp The JSONObject containing the in-app notification data
     * @property inAppId The unique identifier of the in-app campaign
     */
    data class Success(
        val inApp: JSONObject,
        val inAppId: String
    ) : DelayedInAppResult()

    /**
     * Error state - in-app retrieval failed or data is unavailable
     *
     * @property reason The error reason explaining why the callback failed
     * @property inAppId The unique identifier of the in-app campaign (if available)
     * @property throwable Optional throwable for debugging purposes
     */
    data class Error(
        val reason: ErrorReason,
        val inAppId: String = "",
        val throwable: Throwable? = null
    ) : DelayedInAppResult() {

        /**
         * Enum representing specific error reasons
         */
        enum class ErrorReason(val message: String) {
            NOT_FOUND_IN_DB("Delayed in-app not found in database"),
            STORE_NOT_INITIALIZED("DelayedLegacyInAppStore is not initialized"),
            INVALID_JSON("Retrieved data is not a valid JSONObject"),
            CANCELLED("Delayed in-app callback was cancelled"),
            EMPTY_INAPP_ID("InApp ID is empty or invalid"),
            UNKNOWN("Unknown error occurred");

            override fun toString(): String = message
        }
    }
}