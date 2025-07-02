package com.clevertap.android.sdk.network.api

import org.json.JSONArray
import org.json.JSONObject

class ContentFetchRequestBody(
    val header: JSONObject, val items: JSONArray
) {
    override fun toString(): String = "[${header},${items.toString().substring(1)}"
}