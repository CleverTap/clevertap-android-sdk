package com.clevertap.android.sdk.utils

import org.json.JSONObject

fun JSONObject.putObject(name: String, init: JSONObject.() -> Unit) {
    val obj = JSONObject()
    init(obj)
    put(name, obj)
}
