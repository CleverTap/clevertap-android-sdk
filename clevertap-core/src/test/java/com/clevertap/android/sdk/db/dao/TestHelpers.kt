package com.clevertap.android.sdk.db.dao

import org.json.JSONObject

/**
 * Helper extension function to safely get the first key from JSONObject
 * This addresses the Kotlin type safety issue with JSONObject.keys().next()
 */
internal fun JSONObject.getFirstKey(): String {
    return this.keys().next() as String
}

/**
 * Helper extension function to get the last ID and event array from fetchEvents result
 * This is a common pattern used across multiple test files
 */
internal fun JSONObject.getEventsArray(): Pair<String, org.json.JSONArray> {
    val lastId = getFirstKey()
    val eventArray = getJSONArray(lastId)
    return Pair(lastId, eventArray)
}
