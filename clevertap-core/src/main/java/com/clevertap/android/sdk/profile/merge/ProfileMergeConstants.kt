package com.clevertap.android.sdk.profile.merge

/**
 * Constants used throughout the profile merge system.
 */
internal object ProfileMergeConstants {
    /**
     * Special marker to indicate deletion intent.
     * Use this value to mark fields for deletion.
     */
    const val DELETE_MARKER = "__CLEVERTAP_DELETE__"

    /**
     * Prefix for date values that should be converted to long.
     */
    const val DATE_PREFIX = "\$D_"

    /**
     * Checks if a value is the DELETE_MARKER.
     */
    fun isDeleteMarker(value: Any?): Boolean {
        return value is String && value == DELETE_MARKER
    }

    /**
     * Processes a string value, removing $D_ prefix if present and converting to long.
     *
     * @param value The string value to process
     * @return Long value if string starts with $D_ prefix, otherwise original string
     */
    fun processDatePrefix(value: String): Any {
        return if (value.startsWith(DATE_PREFIX)) {
            try {
                value.removePrefix(DATE_PREFIX).toLong()
            } catch (e: NumberFormatException) {
                // If conversion fails, return original string
                value
            }
        } else {
            value
        }
    }
}
