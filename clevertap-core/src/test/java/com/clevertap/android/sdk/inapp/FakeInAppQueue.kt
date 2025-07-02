package com.clevertap.android.sdk.inapp

import org.json.JSONArray
import org.json.JSONObject

internal class FakeInAppQueue : InAppQueue {

    private val queue = mutableListOf<JSONObject>()

    override fun enqueue(jsonObject: JSONObject) {
        queue.add(jsonObject)
    }

    override fun enqueueAll(jsonArray: JSONArray) {
        for (i in 0 until jsonArray.length()) {
            queue.add(jsonArray.getJSONObject(i))
        }
    }

    override fun insertInFront(jsonObject: JSONObject) {
        queue.add(0, jsonObject)
    }

    override fun dequeue(): JSONObject? {
        return queue.removeFirstOrNull()
    }

    override fun getQueueLength(): Int {
        return queue.size
    }
}
