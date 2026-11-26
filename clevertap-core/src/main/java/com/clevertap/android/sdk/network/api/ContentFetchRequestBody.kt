package com.clevertap.android.sdk.network.api

import androidx.annotation.RestrictTo
import org.json.JSONArray
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ContentFetchRequestBody(
    val header: JSONObject, val items: JSONArray
) {
    override fun toString(): String = "[${header},${items.toString().substring(1)}"
}