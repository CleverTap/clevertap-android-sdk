package com.clevertap.android.sdk.variables

import android.text.Editable
import com.clevertap.android.sdk.Logger
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal object JsonUtil {

    @Suppress("UNCHECKED_CAST")
    fun <T> uncheckedCast(obj: Any?): T = obj as T

    fun fromJson(json: String?): MutableMap<String, Any?>? {
        if (json == null) {
            return null
        }
        return try {
            mapFromJson(JSONObject(json))
        } catch (e: JSONException) {
            Logger.v("Error converting $json from JSON", e)
            null
        }
    }

    /**
     * Nullable overload, faithful to the Java original: `null` in, `null` out.
     */
    @JvmName("listToNullableJsonArray")
    fun listToJsonArray(list: Iterable<*>?): JSONArray? = list?.let { listToJsonArray(it) }

    /**
     * Non-null overload: what the existing Kotlin callers (always passing a non-null list) see.
     */
    fun listToJsonArray(list: Iterable<*>): JSONArray {
        val obj = JSONArray()
        for (item in list) {
            obj.put(toJsonValue(item))
        }
        return obj
    }

    /**
     * Non-null overload: what the existing Kotlin callers (always passing a non-null object) see.
     */
    fun <T> mapFromJson(json: JSONObject): MutableMap<String, T> {
        val result: MutableMap<String, T> = HashMap()
        for (key in json.keys()) {
            result[key] = uncheckedCast(fromJsonValue(json.opt(key)))
        }
        return result
    }

    /**
     * Nullable overload, faithful to the Java original: `null` in, `null` out.
     */
    @JvmName("mapFromNullableJson")
    fun <T> mapFromJson(json: JSONObject?): MutableMap<String, T>? = json?.let { mapFromJson<T>(it) }

    fun <T> listFromJsonFromDefault(json: JSONArray?): MutableList<T> =
        uncheckedCast(listFromJson<Any?>(json) ?: mutableListOf<Any?>())

    fun <T> listFromJson(json: JSONArray?): MutableList<T>? = json?.let { listFromJsonSafe(it) }

    fun <T> listFromJsonSafe(json: JSONArray): MutableList<T> {
        val result: MutableList<Any?> = ArrayList(json.length())
        for (i in 0 until json.length()) {
            result.add(fromJsonValue(json.opt(i)))
        }
        return uncheckedCast(result)
    }

    /** JSON value -> plain value: JSONObject.NULL becomes null, objects/arrays become maps/lists. */
    private fun fromJsonValue(value: Any?): Any? = when {
        value == null || value === JSONObject.NULL -> null
        value is JSONObject -> mapFromJson<Any?>(value)
        value is JSONArray -> listFromJson<Any?>(value)
        JSONObject.NULL == value -> null
        else -> value
    }

    /** Plain value -> JSON value (maps/iterables are converted; null becomes JSONObject.NULL). */
    private fun toJsonValue(value: Any?): Any? = when (value) {
        is Map<*, *> -> mapToJsonObject(uncheckedCast<Map<String, *>>(value))
        is Iterable<*> -> listToJsonArray(value)
        null -> JSONObject.NULL
        else -> value
    }

    private fun mapToJsonObject(map: Map<String, *>): JSONObject {
        val obj = JSONObject()
        for ((key, value) in map) {
            obj.put(key, if (value is Editable) value.toString() else toJsonValue(value))
        }
        return obj
    }

    fun toJson(map: Map<String, *>?): String? {
        if (map == null) {
            return null
        }
        return try {
            mapToJsonObject(map).toString()
        } catch (e: JSONException) {
            Logger.v("Error converting $map to JSON", e)
            null
        }
    }

    fun toJson(list: List<*>?): String? {
        if (list == null) {
            return null
        }
        return try {
            listToJsonArray(list).toString()
        } catch (e: JSONException) {
            Logger.v("Error converting $list to JSON", e)
            null
        }
    }
}
