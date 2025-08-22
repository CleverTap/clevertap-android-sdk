package com.clevertap.android.sdk.db

import org.json.JSONArray

/**
 * QueueData that holds event data along with cleanup information
 * Used to track which events were fetched for later cleanup
 */
class QueueData {
    var data: JSONArray? = null
    var eventIds: List<String> = emptyList()  // IDs from events table
    var profileEventIds: List<String> = emptyList()  // IDs from profileEvents table

    val isEmpty: Boolean
        get() = data == null || (data?.length() ?: 0) <= 0

    val hasEvents: Boolean
        get() = eventIds.isNotEmpty()

    val hasProfileEvents: Boolean
        get() = profileEventIds.isNotEmpty()

    override fun toString(): String {
        val numItems = data?.length() ?: 0
        return "QueueData: numItems=$numItems, eventIds=${eventIds.size}, profileEventIds=${profileEventIds.size}"
    }
}