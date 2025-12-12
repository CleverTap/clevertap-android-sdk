package com.clevertap.android.sdk.profile.merge

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject

/**
 * Utilities for working with JSON arrays during profile merging.
 * Handles array copying, element checks, and array-specific operations.
 */
internal object ArrayMergeUtils {

    /**
     * Creates a deep copy of a JSONArray.
     * Uses JSON serialization for a complete copy.
     *
     * @param array The array to copy
     * @return A new JSONArray with the same contents
     */
    fun copyArray(array: JSONArray): JSONArray {
        return JSONArray(array.toString())
    }

    /**
     * Checks if array contains a specific string value.
     *
     * @param array The array to search
     * @param value The string value to find
     * @return true if the array contains the string
     */
    fun arrayContainsString(array: JSONArray, value: String): Boolean {
        for (i in 0 until array.length()) {
            val item = array.get(i)
            if (item is String && item == value) {
                return true
            }
        }
        return false
    }

    /**
     * Checks if array has any elements marked with DELETE_MARKER.
     *
     * @param array The array to check
     * @return true if any element is a delete marker
     */
    fun hasDeleteMarkerElements(array: JSONArray): Boolean {
        return (0 until array.length()).any { i ->
            val element = array.opt(i)
            element is String && element == Constants.DELETE_MARKER
        }
    }

    /**
     * Checks if array has any JSONObject elements (excluding delete markers).
     *
     * @param array The array to check
     * @return true if array contains JSONObject elements
     */
    fun hasJsonObjectElements(array: JSONArray): Boolean {
        return (0 until array.length()).any { i ->
            val element = array.opt(i)
            element is JSONObject && !ProfileOperationUtils.isDeleteMarker(element)
        }
    }

    /**
     * Determines if array should be merged element-wise or replaced entirely.
     * Arrays with JSONObject elements should be merged, others replaced.
     *
     * @param array The array to check
     * @return true if array should be merged element-wise
     */
    fun shouldMergeArrayElements(array: JSONArray): Boolean {
        return (0 until array.length()).any { i -> array.opt(i) is JSONObject }
    }
}
