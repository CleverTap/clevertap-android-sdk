package com.clevertap.android.sdk.utils

import androidx.annotation.RestrictTo
import android.os.Parcel
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun JSONObject.putObject(name: String, init: JSONObject.() -> Unit) {
    val obj = JSONObject()
    init(obj)
    put(name, obj)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun JSONObject.getStringOrNull(name: String): String? {
    return if (has(name)) {
        getString(name)
    } else {
        null
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun JSONArray.prepend(value: Any) {
    var index = 0
    var shiftItem: Any = value
    while (index < length()) {
        val currentItem = get(index)
        put(index, shiftItem)
        shiftItem = currentItem
        index++
    }
    put(shiftItem)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun Parcel.readJson(): JSONObject? {
    return try {
        readString()?.let { JSONObject(it) }
    } catch (je: JSONException) {
        null
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun Parcel.writeJson(json: JSONObject?) {
    writeString(json?.toString())
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun JSONArray.filterObjects(predicate: (element: JSONObject) -> Boolean): JSONArray {
    val filteredArray = JSONArray()
    for (i in 0 until length()) {
        val element = optJSONObject(i)
        if (element != null && predicate(element)) {
            filteredArray.put(element)
        }
    }
    return filteredArray
}
