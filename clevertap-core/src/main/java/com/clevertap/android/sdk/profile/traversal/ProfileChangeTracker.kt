package com.clevertap.android.sdk.profile.traversal

import com.clevertap.android.sdk.utils.DataProcessingUtils
import org.json.JSONObject

/**
 * Tracks changes made during profile merging operations.
 * Records additions, updates, and deletions with dot-notation paths.
 * Automatically processes date prefixes for all recorded values.
 */
internal class ProfileChangeTracker {

    /**
     * Processes date prefixes in a value before recording.
     * Delegates to DataProcessingUtils for the actual processing.
     */
    private fun processValue(value: Any?): Any? {
        return value?.let { DataProcessingUtils.processDatePrefixes(it) }
    }

    /**
     * Records all leaf values in a JSONObject as additions (oldValue = null).
     * Recursively traverses nested objects.
     * Automatically processes date prefixes for all values.
     *
     * @param jsonObject The object to record
     * @param basePath The dot-notation path prefix
     * @param changes The map to accumulate changes in
     */
    private fun recordAllLeafAdditions(
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
                is JSONObject -> recordAllLeafAdditions(value, currentPath, changes)
                else -> changes[currentPath] = ProfileChange(null, processValue(value))
            }
        }
    }

    /**
     * Records all leaf values in a JSONObject as deletions (newValue = null).
     * Recursively traverses nested objects.
     * Automatically processes date prefixes for all values.
     *
     * @param jsonObject The object to record
     * @param basePath The dot-notation path prefix
     * @param changes The map to accumulate changes in
     */
    private fun recordAllLeafDeletions(
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
                else -> changes[currentPath] = ProfileChange(processValue(value), null)
            }
        }
    }

    /**
     * Records a deletion of a value.
     * If the value is a JSONObject, records all leaf deletions.
     * Automatically processes date prefixes for the value.
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
            changes[path] = ProfileChange(processValue(value), null)
        }
    }

    /**
     * Records a change with both old and new values.
     * Automatically processes date prefixes for both values.
     *
     * @param path The dot-notation path
     * @param oldValue The previous value
     * @param newValue The new value
     * @param changes The map to accumulate changes in
     */
    fun recordChange(
        path: String,
        oldValue: Any?,
        newValue: Any?,
        changes: MutableMap<String, ProfileChange>
    ) {
        changes[path] = ProfileChange(processValue(oldValue), processValue(newValue))
    }

    /**
     * Records an addition (oldValue = null, newValue provided).
     * Automatically processes date prefixes for the new value.
     * If newValue is a JSONObject, recursively records all leaf values.
     *
     * @param path The dot-notation path
     * @param newValue The new value being added
     * @param changes The map to accumulate changes in
     */
    fun recordAddition(
        path: String,
        newValue: Any,
        changes: MutableMap<String, ProfileChange>
    ) {
        val processedValue = processValue(newValue)
        if (processedValue is JSONObject) {
            recordAllLeafAdditions(processedValue, path, changes)
        } else {
            changes[path] = ProfileChange(null, processedValue)
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
