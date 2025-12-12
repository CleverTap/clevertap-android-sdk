package com.clevertap.android.sdk.profile.traversal

import org.json.JSONObject

/**
 * Tracks changes made during profile merging operations.
 * Records additions, updates, and deletions with dot-notation paths.
 */
internal class ProfileChangeTracker {

    /**
     * Records all leaf values in a JSONObject as additions (oldValue = null).
     * Recursively traverses nested objects.
     *
     * @param jsonObject The object to record
     * @param basePath The dot-notation path prefix
     * @param changes The map to accumulate changes in
     */
    fun recordAllLeafValues(
        jsonObject: JSONObject,
        basePath: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.get(key)
            val currentPath = buildPath(basePath, key)

            when (value) {
                is JSONObject -> recordAllLeafValues(value, currentPath, changes)
                else -> changes[currentPath] = ProfileChange(null, value)
            }
        }
    }

    /**
     * Records all leaf values in a JSONObject as deletions (newValue = null).
     * Recursively traverses nested objects.
     *
     * @param jsonObject The object to record
     * @param basePath The dot-notation path prefix
     * @param changes The map to accumulate changes in
     */
    fun recordAllLeafDeletions(
        jsonObject: JSONObject,
        basePath: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.get(key)
            val currentPath = buildPath(basePath, key)

            when (value) {
                is JSONObject -> recordAllLeafDeletions(value, currentPath, changes)
                else -> changes[currentPath] = ProfileChange(value, null)
            }
        }
    }

    /**
     * Records a deletion of a value.
     * If the value is a JSONObject, records all leaf deletions.
     *
     * @param value The value being deleted
     * @param path The dot-notation path
     * @param changes The map to accumulate changes in
     */
    fun recordDeletion(
        value: Any,
        path: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        if (value is JSONObject) {
            recordAllLeafDeletions(value, path, changes)
        } else {
            changes[path] = ProfileChange(value, null)
        }
    }

    /**
     * Builds a dot-notation path from base path and key.
     *
     * @param basePath The base path (can be empty)
     * @param key The key to append
     * @return The combined path
     */
    private fun buildPath(basePath: String, key: String): String {
        return if (basePath.isEmpty()) key else "$basePath.$key"
    }
}

/**
 * Represents a change in profile state, tracking old and new values.
 *
 * @property oldValue The previous value (null if newly added)
 * @property newValue The new value (null if deleted)
 */
data class ProfileChange(
    val oldValue: Any?,
    val newValue: Any?
)
