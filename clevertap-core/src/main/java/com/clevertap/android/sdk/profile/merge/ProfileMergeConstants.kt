package com.clevertap.android.sdk.profile.merge

import com.clevertap.android.sdk.Constants

/**
 * Constants used throughout the profile merge system.
 */
internal object ProfileMergeConstants {
    /**
     * Processes a string value, removing $D_ prefix if present and converting to long.
     *
     * @param value The string value to process
     * @return Long value if string starts with $D_ prefix, otherwise original string
     */
    fun processDatePrefix(value: String): Any {
        return if (value.startsWith(Constants.DATE_PREFIX)) {
            try {
                value.removePrefix(Constants.DATE_PREFIX).toLong()
            } catch (e: NumberFormatException) {
                // If conversion fails, return original string
                value
            }
        } else {
            value
        }
    }

    /**
     * Checks if a value is the DELETE_MARKER.
     */
    fun isDeleteMarker(value: Any?): Boolean {
        return value is String && value == Constants.DELETE_MARKER
    }
}
