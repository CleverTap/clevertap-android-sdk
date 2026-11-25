package com.clevertap.android.sdk.variables

import android.text.Editable
import com.clevertap.android.sdk.Logger
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal object JsonUtil {

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T> uncheckedCast(obj: Any?): T {
        return obj as T
    }

    @JvmStatic
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
    @JvmStatic
    @JvmName("listToNullableJsonArray")
    @Throws(JSONException::class)
    fun listToJsonArray(list: Iterable<*>?): JSONArray? {
        if (list == null) {
            return null
        }
        return listToJsonArray(list)
    }

    /**
     * Non-null overload: what the existing Kotlin callers (always passing a non-null list) see.
     */
    @JvmStatic
    @Throws(JSONException::class)
    fun listToJsonArray(list: Iterable<*>): JSONArray {
        val obj = JSONArray()
        for (item in list) {
            var value: Any? = item
            if (value is Map<*, *>) {
                val mappedValue: Map<String, *> = uncheckedCast(value)
                value = mapToJsonObject(mappedValue)
            } else if (value is Iterable<*>) {
                value = listToJsonArray(value)
            } else if (value == null) {
                value = JSONObject.NULL
            }
            obj.put(value)
        }
        return obj
    }

    /**
     * Non-null overload: what the existing Kotlin callers (always passing a non-null object) see.
     */
    @JvmStatic
    fun <T> mapFromJson(`object`: JSONObject): MutableMap<String, T> {
        val result: MutableMap<String, T> = HashMap()
        val keysIterator: Iterator<*> = `object`.keys()
        while (keysIterator.hasNext()) {
            val key = keysIterator.next() as String
            var value: Any? = `object`.opt(key)
            if (value == null || value === JSONObject.NULL) {
                value = null
            } else if (value is JSONObject) {
                value = mapFromJson<Any?>(value)
            } else if (value is JSONArray) {
                value = listFromJson<Any?>(value)
            } else if (JSONObject.NULL == value) {
                value = null
            }
            val castedValue: T = uncheckedCast(value)
            result[key] = castedValue
        }
        return result
    }

    /**
     * Nullable overload, faithful to the Java original: `null` in, `null` out.
     */
    @JvmStatic
    @JvmName("mapFromNullableJson")
    fun <T> mapFromJson(`object`: JSONObject?): MutableMap<String, T>? {
        if (`object` == null) {
            return null
        }
        return mapFromJson<T>(`object`)
    }

    @JvmStatic
    fun <T> listFromJsonFromDefault(json: JSONArray?): MutableList<T> {
        val objects: List<Any?>? = listFromJson(json)
        return uncheckedCast(objects ?: ArrayList<Any?>())
    }

    @JvmStatic
    fun <T> listFromJson(json: JSONArray?): MutableList<T>? {
        if (json == null) {
            return null
        }
        return listFromJsonSafe(json)
    }

    @JvmStatic
    fun <T> listFromJsonSafe(json: JSONArray): MutableList<T> {
        val result: MutableList<Any?> = ArrayList(json.length())
        for (i in 0 until json.length()) {
            var value: Any? = json.opt(i)
            if (value == null || value === JSONObject.NULL) {
                value = null
            } else if (value is JSONObject) {
                value = mapFromJson<Any?>(value)
            } else if (value is JSONArray) {
                value = listFromJson<Any?>(value)
            } else if (JSONObject.NULL == value) {
                value = null
            }
            result.add(value)
        }
        return uncheckedCast(result)
    }

    @Throws(JSONException::class)
    private fun mapToJsonObject(map: Map<String, *>?): JSONObject? {
        if (map == null) {
            return null
        }
        val obj = JSONObject()
        for ((key, entryValue) in map) {
            var value: Any? = entryValue
            if (value is Map<*, *>) {
                val mappedValue: Map<String, *> = uncheckedCast(value)
                value = mapToJsonObject(mappedValue)
            } else if (value is Iterable<*>) {
                value = listToJsonArray(value)
            } else if (value is Editable) {
                value = value.toString()
            } else if (value == null) {
                value = JSONObject.NULL
            }
            obj.put(key, value)
        }
        return obj
    }

    @JvmStatic
    fun toJson(map: Map<String, *>?): String? {
        if (map == null) {
            return null
        }
        return try {
            mapToJsonObject(map)!!.toString()
        } catch (e: JSONException) {
            Logger.v("Error converting $map to JSON", e)
            null
        }
    }

    @JvmStatic
    fun toJson(list: List<*>?): String? {
        if (list == null) {
            return null
        }
        return try {
            listToJsonArray(list)!!.toString()
        } catch (e: JSONException) {
            Logger.v("Error converting $list to JSON", e)
            null
        }
    }
}
