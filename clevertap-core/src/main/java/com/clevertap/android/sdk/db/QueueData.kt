package com.clevertap.android.sdk.db

import org.json.JSONArray

/**
 * QueueData that holds event data along with cleanup information
 * Used to track which events were fetched for later cleanup
 */
class QueueData {
    val data: JSONArray = JSONArray()
    val eventIds: MutableList<String> = mutableListOf()  // IDs from events table
    val profileEventIds: MutableList<String> = mutableListOf()  // IDs from profileEvents table

    val isEmpty: Boolean
        get() = data.length() <= 0

    val hasEvents: Boolean
        get() = eventIds.isNotEmpty()

    val hasProfileEvents: Boolean
        get() = profileEventIds.isNotEmpty()

    override fun toString(): String {
        return "QueueData: numItems=${data.length()}, eventIds=${eventIds.size}, profileEventIds=${profileEventIds.size}"
    }
}