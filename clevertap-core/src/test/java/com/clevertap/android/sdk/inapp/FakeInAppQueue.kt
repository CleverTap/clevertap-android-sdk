package com.clevertap.android.sdk.inapp

import org.json.JSONObject

internal class FakeInAppQueue : InAppQueue {

    private val queue = mutableListOf<JSONObject>()

    override fun enqueue(inApp: JSONObject) {
        queue.add(inApp)
    }

    override fun enqueueAll(inApps: List<JSONObject>) {
        queue.addAll(inApps)
    }

    override fun insertInFront(inApp: JSONObject) {
        queue.add(0, inApp)
    }

    override fun dequeue(): JSONObject? {
        return queue.removeFirstOrNull()
    }

    override fun getQueueLength(): Int {
        return queue.size
    }
}
