package com.clevertap.android.sdk.utils

import org.json.JSONArray
import org.json.JSONObject

object JsonFlattener {

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
    fun flattenInternal(json: JSONObject, prefix: String): Map<String, Any> {
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
                    result[newKey] = value
                }
                JSONObject.NULL -> {
                    // Skip null values or include them based on preference
                    // result[newKey] = null
                }
                else -> {
                    // Primitive values (String, Number, Boolean, Date, etc.)
                    result[newKey] = value
                }
            }
        }

        return result
    }
}