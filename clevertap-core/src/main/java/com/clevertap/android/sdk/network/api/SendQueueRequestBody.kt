package com.clevertap.android.sdk.network.api

import androidx.annotation.RestrictTo
import org.json.JSONArray
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class SendQueueRequestBody(
    val queueHeader: JSONObject?,
    val queue: JSONArray
) {

    override fun toString(): String = if (queueHeader == null) {
        queue.toString()
    } else {
        // prepend header to the queue array
        "[${queueHeader.toString()},${queue.toString().substring(1)}"
    }
}
