package com.clevertap.android.sdk.network.fetch

import androidx.annotation.RestrictTo
import org.json.JSONArray
import org.json.JSONObject

/**
 * Generic POST body for direct V2 endpoint calls.
 *
 * Format: `[header, event]` — same layout the existing body classes produce,
 * but decoupled from any specific event shape. Callers build their own
 * [event] (`wzrk_fetch`, `Message Deleted`, etc.) and pass it in.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class EventRequestBody(
    private val header: JSONObject,
    private val event: JSONObject
) {
    fun toJsonString(): String =
        JSONArray().put(header).put(event).toString()
}
