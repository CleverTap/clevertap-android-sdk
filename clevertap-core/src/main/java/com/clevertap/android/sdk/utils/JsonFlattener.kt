package com.clevertap.android.sdk.utils

import org.json.JSONArray
import org.json.JSONObject

internal object JsonFlattener {

    @JvmStatic
    fun flatten(json: JSONObject): Map<String, Any> {
        return flattenInternal(json, "")
    }


    /**
     * Flattens a nested JSONObject into a single-level Map with dot-notation keys
     * @param json The JSONObject to flatten
     * @param prefix Optional prefix for keys (used in recursion)
     * @return Flattened map with dot-notation keys
     */
    @JvmStatic
    private fun flattenInternal(json: JSONObject, prefix: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        json.keys().forEach { key ->
            val value = json.get(key)
            val newKey = if (prefix.isEmpty()) key else "$prefix.$key"

            when (value) {
                is JSONObject -> {
                    // Recursively flatten nested objects
                    result.putAll(flattenInternal(value, newKey))
                }
                is JSONArray -> {
                    // Keep JSONArray as-is
                    result[newKey] = DataProcessingUtils.processDatePrefixes(value)
                }
                JSONObject.NULL -> {
                    // no-op
                }
                is String -> {
                    result[newKey] = DataProcessingUtils.processDatePrefixes(value)
                }
                else -> {
                    // Primitive values (String, Number, Boolean etc.)
                    result[newKey] = value
                }
            }
        }

        return result
    }
}