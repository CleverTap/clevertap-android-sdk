package com.clevertap.android.sdk.profile.traversal

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject

/**
 * Creates a deep copy of a JSONArray.
 * Uses JSON serialization for a complete copy.
 *
 * @return A new JSONArray with the same contents
 */
internal fun JSONArray.deepCopy(): JSONArray {
    return JSONArray(this.toString())
}

/**
 * Checks if array contains a specific string value.
 *
 * @param value The string value to find
 * @return true if the array contains the string
 */
internal fun JSONArray.containsString(value: String): Boolean {
    return (0 until this.length()).any { i ->
        val item = this.get(i)
        item is String && item == value
    }
}

/**
 * Checks if array has any elements marked with DELETE_MARKER.
 *
 * @return true if any element is a delete marker
 */
internal fun JSONArray.hasDeleteMarkerElements(): Boolean {
    return (0 until this.length()).any { i ->
        val element = this.opt(i)
        element is String && element == Constants.DELETE_MARKER
    }
}

/**
 * Checks if array has any JSONObject elements (excluding delete markers).
 *
 * @return true if array contains JSONObject elements
 */
internal fun JSONArray.hasJsonObjectElements(): Boolean {
    return (0 until this.length()).any { i ->
        val element = this.opt(i)
        element is JSONObject
    }
}
