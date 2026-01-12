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

        val hasDeleteMarkers = ArrayMergeUtils.hasDeleteMarkerElements(newArray)
        val hasObjectsToDelete = ArrayMergeUtils.hasJsonObjectElements(newArray)

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
        val oldArrayCopy = ArrayMergeUtils.copyArray(oldArray)
        var arrayModified = false

        for (i in 0 until minOf(newArray.length(), oldArray.length())) {
            val oldElement = oldArray.get(i)
            val newElement = newArray.get(i)

            if (oldElement is JSONObject && newElement is JSONObject) {
                // Recursively delete fields from this array element
                val elementHandler = DeleteOperationHandler(changeTracker)
                val elementKeys = newElement.keys()
                while (elementKeys.hasNext()) {
                    val key = elementKeys.next()
                    val value = newElement.get(key)
                    elementHandler.handleDelete(
                        oldElement,
                        key,
                        value,
                        "",
                        mutableMapOf()
                    ) { target, source, _, _ ->
                        if (source != null) {
                            elementHandler.handleDeleteRecursive(target, source, mutableMapOf())
                        }
                    }
                }
                arrayModified = true

                // Remove empty objects
                if (oldElement.length() == 0) {
                    oldArray.remove(i)
                }
            }
        }

        if (arrayModified) {
            changes[basePath] = ProfileChange(oldArrayCopy, oldArray)
        }
    }

    /**
     * Helper for recursive deletion within nested objects.
     */
    private fun handleDeleteRecursive(
        target: JSONObject,
        source: JSONObject,
        changes: MutableMap<String, ProfileChange>
    ) {
        val keys = source.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val newValue = source.get(key)
            handleDelete(target, key, newValue, "", changes) { t, s, _, _ ->
                if (s != null) {
                    handleDeleteRecursive(t, s, mutableMapOf())
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

        val oldArrayCopy = ArrayMergeUtils.copyArray(oldArray)
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
            val processedOldArrayCopy = ProfileOperationUtils.processDatePrefixes(oldArrayCopy)
            val processedOldArray = ProfileOperationUtils.processDatePrefixes(oldArray)
            changes[basePath] = ProfileChange(processedOldArrayCopy, processedOldArray)
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
        val processedValue = ProfileOperationUtils.processDatePrefixes(value)
        changeTracker.recordDeletion(processedValue, path, changes)
        parent.remove(key)
    }
}

