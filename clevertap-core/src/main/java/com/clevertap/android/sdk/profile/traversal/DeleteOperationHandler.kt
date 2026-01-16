package com.clevertap.android.sdk.profile.traversal

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Handles DELETE operation during profile merging.
 * Supports deleting keys, nested objects, array elements, and fields within array elements.
 */
internal class DeleteOperationHandler(
    private val changeTracker: ProfileChangeTracker
) {

    /**
     * Handles deletion operations for a key in the target object.
     *
     * @param target The JSON object to delete from
     * @param key The key to process
     * @param newValue The value from source (may contain delete markers or nested deletions)
     * @param currentPath The dot-notation path
     * @param changes The map to accumulate changes in
     * @param recursiveMerge Function to recursively merge nested objects
     */
    @Throws(JSONException::class)
    fun handleDelete(
        target: JSONObject,
        key: String,
        newValue: Any,
        currentPath: String,
        changes: MutableMap<String, ProfileChange>,
        recursiveMerge: (JSONObject, JSONObject?, String, MutableMap<String, ProfileChange>) -> Unit
    ) {
        if (!target.has(key)) return

        val oldValue = target.get(key)

        when {
            ProfileOperationUtils.isDeleteMarker(newValue) -> {
                // Delete this key entirely
                deleteValue(target, key, oldValue, currentPath, changes)
            }
            oldValue is JSONObject && newValue is JSONObject -> {
                // Recurse into nested objects for deletion
                recursiveMerge(oldValue, newValue, currentPath, changes)
                // Remove the object if it's now empty
                if (oldValue.length() == 0) {
                    target.remove(key)
                }
            }
            oldValue is JSONArray && newValue is JSONArray -> {
                // Handle array element deletions
                handleArrayDeletion(target, key, oldValue, newValue, currentPath, changes)
            }
        }
    }

    /**
     * Handles array deletions (element removal or field deletion from array elements).
     */
    private fun handleArrayDeletion(
        parentJson: JSONObject,
        key: String,
        oldArray: JSONArray,
        newArray: JSONArray,
        currentPath: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        if (newArray.length() == 0) return

        val hasDeleteMarkers = newArray.hasDeleteMarkerElements()
        val hasObjectsToDelete = newArray.hasJsonObjectElements()

        when {
            hasDeleteMarkers -> deleteArrayElements(oldArray, newArray, currentPath, changes)
            hasObjectsToDelete -> deleteFromArrayElements(oldArray, newArray, currentPath, changes)
            else -> deleteValue(parentJson, key, oldArray, currentPath, changes)
        }
    }

    /**
     * Deletes specific fields from array elements.
     * Each element in newArray specifies which fields to delete from corresponding oldArray element.
     */
    private fun deleteFromArrayElements(
        oldArray: JSONArray,
        newArray: JSONArray,
        basePath: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        val oldArrayCopy = oldArray.deepCopy()
        var arrayModified = false
        val indicesToRemove = mutableListOf<Int>()

        for (i in 0 until minOf(newArray.length(), oldArray.length())) {
            val oldElement = oldArray.get(i)
            val newElement = newArray.get(i)

            if (oldElement is JSONObject && newElement is JSONObject) {
                val elementKeys = newElement.keys()
                while (elementKeys.hasNext()) {
                    val key = elementKeys.next()
                    val value = newElement.get(key)
                    handleDelete(
                        oldElement,
                        key,
                        value,
                        "",
                        mutableMapOf()
                    ) { target, source, _, _ ->
                        if (source != null) {
                           handleDeleteRecursive(target, source)
                        }
                    }
                }
                arrayModified = true

                // Mark for removal if empty
                if (oldElement.length() == 0) {
                    indicesToRemove.add(i)
                }
            }
        }

        // Remove in reverse order
        indicesToRemove.sortedDescending().forEach { index ->
            oldArray.remove(index)
        }

        if (arrayModified) {
            changeTracker.recordChange(basePath, oldArrayCopy, oldArray, changes)
        }
    }

    /**
     * Helper for recursive deletion within nested objects.
     */
    private fun handleDeleteRecursive(
        target: JSONObject,
        source: JSONObject,
    ) {
        val keys = source.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val newValue = source.get(key)
            handleDelete(target, key, newValue, "", mutableMapOf()) { t, s, _, _ ->
                if (s != null) {
                    handleDeleteRecursive(t, s)
                }
            }
        }
    }

    /**
     * Deletes entire array elements at specific indices marked with DELETE_MARKER.
     */
    private fun deleteArrayElements(
        oldArray: JSONArray,
        newArray: JSONArray,
        basePath: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        val indicesToDelete = mutableListOf<Int>()

        // Collect indices to delete
        for (i in 0 until newArray.length()) {
            if (ProfileOperationUtils.isDeleteMarker(newArray.opt(i)) && i < oldArray.length()) {
                indicesToDelete.add(i)
            }
        }

        if (indicesToDelete.isEmpty()) return

        val oldArrayCopy = oldArray.deepCopy()
        var removedAny = false

        // Delete in reverse order to maintain correct indices
        indicesToDelete.sortedDescending().forEach { index ->
            val oldElement = oldArray.get(index)
            // check is needed since BE can only delete leaf nodes
            if (oldElement !is JSONObject && oldElement !is JSONArray) {
                oldArray.remove(index)
                removedAny = true
            }
        }

        // Only report changes if we actually removed something
        if (removedAny) {
            changeTracker.recordChange(basePath, oldArrayCopy, oldArray, changes)
        }
    }

    /**
     * Deletes a value and records the change.
     */
    private fun deleteValue(
        parent: JSONObject,
        key: String,
        value: Any,
        path: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        if (value is JSONArray || value is JSONObject) {
            // check is needed since BE can only delete leaf nodes
            return
        }
        changeTracker.recordDeletion(value, path, changes)
        parent.remove(key)
    }
}

