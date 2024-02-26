package com.clevertap.android.sdk.network.api

import org.json.JSONArray
import org.json.JSONObject

class SendQueueRequestBody(var queueHeader: JSONObject?, var queue: JSONArray) {

    override fun toString(): String = if (queueHeader == null) {
        queue.toString()
    } else {
        // prepend header to the queue array
        "[${queueHeader.toString()},${queue.toString().substring(1)}"
    }
}
