package com.clevertap.android.sdk.db

import org.json.JSONArray

/**
 * QueueData that holds event data along with cleanup information
 * Used to track which events were fetched for later cleanup
 */
internal class QueueData {
    internal val data: JSONArray = JSONArray()
    internal val eventIds: MutableList<String> = mutableListOf()  // IDs from events table
    internal val profileEventIds: MutableList<String> = mutableListOf()  // IDs from profileEvents table
    internal var hasMore: Boolean = false

    internal val isEmpty: Boolean
        get() = data.length() <= 0

    internal val hasEvents: Boolean
        get() = eventIds.isNotEmpty()

    internal val hasProfileEvents: Boolean
        get() = profileEventIds.isNotEmpty()

    override fun toString(): String {
        return "QueueData: numItems=${data.length()}, eventIds=${eventIds.size}, profileEventIds=${profileEventIds.size}"
    }
}