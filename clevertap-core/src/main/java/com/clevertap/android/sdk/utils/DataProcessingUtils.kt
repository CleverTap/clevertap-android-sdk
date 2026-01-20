package com.clevertap.android.sdk.utils

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject

/**
 * Constants used throughout the profile merge system.
 */
internal object DataProcessingUtils {
    /**
     * Processes a string value, removing $D_ prefix if present and converting to long.
     *
     * @param value The string value to process
     * @return Long value if string starts with $D_ prefix, otherwise original string
     */
    private fun processDatePrefix(value: String): Any {
        return if (value.startsWith(Constants.DATE_PREFIX)) {
            try {
                value.removePrefix(Constants.DATE_PREFIX).toLong()
            } catch (_: NumberFormatException) {
                // If conversion fails, return original string
                value
            }
        } else {
            value
        }
    }

    /**
     * Recursively processes date prefixes in any value type.
     */
    fun processDatePrefixes(value: Any): Any {
        return when (value) {
            is String -> processDatePrefix(value)
            is JSONArray -> processArrayDatePrefixes(value)
            is JSONObject -> processObjectDatePrefixes(value)
            else -> value
        }
    }

    /**
     * Creates a new array with date prefixes processed for all elements.
     */
    private fun processArrayDatePrefixes(array: JSONArray): JSONArray {
        val result = JSONArray()
        for (i in 0 until array.length()) {
            result.put(processDatePrefixes(array.get(i)))
        }
        return result
    }

    /**
     * Creates a new object with date prefixes processed for all values.
     */
    private fun processObjectDatePrefixes(obj: JSONObject): JSONObject {
        val result = JSONObject()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            result.put(key, processDatePrefixes(obj.get(key)))
        }
        return result
    }

    /**
     * Checks if a value is the DELETE_MARKER.
     */
    fun isDeleteMarker(value: Any?): Boolean {
        return value is String && value == Constants.DELETE_MARKER
    }
}