package com.clevertap.android.sdk.utils

import android.os.Parcel
import org.json.JSONException
import org.json.JSONObject

fun JSONObject.putObject(name: String, init: JSONObject.() -> Unit) {
    val obj = JSONObject()
    init(obj)
    put(name, obj)
}

fun JSONObject.getStringOrNull(name: String): String? {
    return if (has(name)) {
        getString(name)
    } else {
        null
    }
}

fun Parcel.readJson(): JSONObject? {
    return try {
        readString()?.let { JSONObject(it) }
    } catch (je: JSONException) {
        null
    }
}

fun Parcel.writeJson(json: JSONObject?) {
    writeString(json?.toString())
}
