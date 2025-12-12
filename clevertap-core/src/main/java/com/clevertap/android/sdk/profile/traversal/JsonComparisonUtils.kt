package com.clevertap.android.sdk.profile.traversal

import org.json.JSONArray
import org.json.JSONObject

/**
 * Utilities for comparing JSON objects and arrays for equality.
 * Provides deep equality checks that work correctly with nested structures.
 */
internal object JsonComparisonUtils {

    /**
     * Deep equality check for JSON values.
     * Handles JSONObject, JSONArray, and primitive types.
     *
     * @param obj1 First value to compare
     * @param obj2 Second value to compare
     * @return true if values are deeply equal
     */
    fun areEqual(obj1: Any?, obj2: Any?): Boolean {
        if (obj1 == null && obj2 == null) return true
        if (obj1 == null || obj2 == null) return false
        if (obj1 === obj2) return true

        return when {
            obj1 is JSONObject && obj2 is JSONObject -> jsonObjectsEqual(obj1, obj2)
            obj1 is JSONArray && obj2 is JSONArray -> jsonArraysEqual(obj1, obj2)
            else -> obj1 == obj2
        }
    }

    /**
     * Checks if two JSONObjects are deeply equal.
     * Compares all keys and recursively checks nested structures.
     */
    private fun jsonObjectsEqual(obj1: JSONObject, obj2: JSONObject): Boolean {
        if (obj1.length() != obj2.length()) return false

        val keys1 = obj1.keys()
        while (keys1.hasNext()) {
            val key = keys1.next()
            if (!obj2.has(key)) return false

            val value1 = obj1.get(key)
            val value2 = obj2.get(key)

            if (!areEqual(value1, value2)) return false
        }

        return true
    }

    /**
     * Checks if two JSONArrays are deeply equal.
     * Compares element-by-element in order.
     */
    private fun jsonArraysEqual(arr1: JSONArray, arr2: JSONArray): Boolean {
        if (arr1.length() != arr2.length()) return false

        for (i in 0 until arr1.length()) {
            val value1 = arr1.get(i)
            val value2 = arr2.get(i)

            if (!areEqual(value1, value2)) return false
        }

        return true
    }
}
