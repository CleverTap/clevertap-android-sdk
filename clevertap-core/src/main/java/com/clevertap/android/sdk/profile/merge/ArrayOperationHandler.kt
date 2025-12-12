package com.clevertap.android.sdk.profile.merge

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Handles array-specific merge operations: ARRAY_ADD, ARRAY_REMOVE, and array element merging.
 * Supports both simple array operations and complex element-wise merging.
 */
internal class ArrayOperationHandler() {

    /**
     * Handles all array merge operations based on the operation type.
     *
     * @param parentJson The parent JSON object containing the array
     * @param key The key of the array in the parent
     * @param oldArray The existing array
     * @param newArray The array with updates
     * @param currentPath The dot-notation path
     * @param changes The map to accumulate changes in
     * @param operation The merge operation type
     * @param recursiveMerge Function to recursively merge nested objects
     */
    @Throws(JSONException::class)
    fun handleArrayMerge(
        parentJson: JSONObject,
        key: String,
        oldArray: JSONArray,
        newArray: JSONArray,
        currentPath: String,
        changes: MutableMap<String, ProfileChange>,
        operation: MergeOperation,
        recursiveMerge: (JSONObject, JSONObject?, String, MutableMap<String, ProfileChange>) -> Unit
    ) {
        if (newArray.length() == 0) return

        when (operation) {
            MergeOperation.ARRAY_ADD -> handleArrayAdd(oldArray, newArray, currentPath, changes)
            MergeOperation.ARRAY_REMOVE -> handleArrayRemove(parentJson, key, oldArray, newArray, currentPath, changes)
            MergeOperation.UPDATE, MergeOperation.INCREMENT, MergeOperation.DECREMENT -> {
                if (ArrayMergeUtils.shouldMergeArrayElements(newArray)) {
                    mergeArrayElements(oldArray, newArray, currentPath, changes, operation, recursiveMerge)
                } else {
                    handleArrayReplacement(parentJson, key, oldArray, newArray, currentPath, changes)
                }
            }
            else -> {}
        }
    }

    /**
     * Adds string values to array (allows duplicates).
     * If a string starts with $D_ prefix, removes it and converts to long.
     */
    private fun handleArrayAdd(
        oldArray: JSONArray,
        newArray: JSONArray,
        path: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        val oldArrayCopy = ArrayMergeUtils.copyArray(oldArray)
        var modified = false

        for (i in 0 until newArray.length()) {
            val item = newArray.get(i)
            if (item is String) {
                val processedItem = ProfileMergeConstants.processDatePrefix(item)
                oldArray.put(processedItem)
                modified = true
            }
        }

        if (modified) {
            changes[path] = ProfileChange(oldArrayCopy, oldArray)
        }
    }

    /**
     * Removes string values from array.
     */
    private fun handleArrayRemove(
        parentJson: JSONObject,
        key: String,
        oldArray: JSONArray,
        newArray: JSONArray,
        path: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        val oldArrayCopy = ArrayMergeUtils.copyArray(oldArray)
        val resultArray = JSONArray()
        var modified = false

        for (i in 0 until oldArray.length()) {
            val item = oldArray.get(i)
            if (item is String && ArrayMergeUtils.arrayContainsString(newArray, item)) {
                modified = true
            } else {
                resultArray.put(item)
            }
        }

        if (modified) {
            parentJson.put(key, resultArray)
            changes[path] = ProfileChange(oldArrayCopy, resultArray)
        }
    }

    /**
     * Replaces entire array with new array.
     */
    private fun handleArrayReplacement(
        parentJson: JSONObject,
        key: String,
        oldArray: JSONArray,
        newArray: JSONArray,
        path: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        if (!JsonComparisonUtils.areEqual(oldArray, newArray)) {
            parentJson.put(key, newArray)
            changes[path] = ProfileChange(oldArray, newArray)
        }
    }

    /**
     * Merges array elements individually.
     * Handles objects, numbers, and simple values differently based on operation.
     */
    private fun mergeArrayElements(
        oldArray: JSONArray,
        newArray: JSONArray,
        basePath: String,
        changes: MutableMap<String, ProfileChange>,
        operation: MergeOperation,
        recursiveMerge: (JSONObject, JSONObject?, String, MutableMap<String, ProfileChange>) -> Unit
    ) {
        val oldArrayCopy = ArrayMergeUtils.copyArray(oldArray)
        var arrayModified = false

        for (i in 0 until newArray.length()) {
            if (i >= oldArray.length()) {
                arrayModified = handleOutOfBoundsIndex(oldArray, newArray, i, operation) || arrayModified
                continue
            }

            val oldElement = oldArray.get(i)
            val newElement = newArray.get(i)

            when {
                oldElement is JSONObject && newElement is JSONObject -> {
                    val elementChanges = mutableMapOf<String, ProfileChange>()
                    recursiveMerge(oldElement, newElement, "", elementChanges)
                    if (elementChanges.isNotEmpty()) {
                        arrayModified = true
                    }
                }
                oldElement is Number && newElement is Number -> {
                    val result = applyNumberOperation(oldElement, newElement, operation)
                    if (!JsonComparisonUtils.areEqual(oldElement, result)) {
                        oldArray.put(i, result)
                        arrayModified = true
                    }
                }
                operation == MergeOperation.UPDATE && !JsonComparisonUtils.areEqual(oldElement, newElement) -> {
                    oldArray.put(i, newElement)
                    arrayModified = true
                }
            }
        }

        if (arrayModified) {
            changes[basePath] = ProfileChange(oldArrayCopy, oldArray)
        }
    }

    /**
     * Handles array indices that are out of bounds.
     * For UPDATE operations, extends the array with NULL values and sets the element.
     *
     * @return true if array was modified
     */
    private fun handleOutOfBoundsIndex(
        oldArray: JSONArray,
        newArray: JSONArray,
        index: Int,
        operation: MergeOperation
    ): Boolean {
        if (operation != MergeOperation.UPDATE) return false

        val newElement = newArray.get(index)
        while (oldArray.length() <= index) {
            oldArray.put(JSONObject.NULL)
        }
        oldArray.put(index, newElement)
        return true
    }

    /**
     * Applies arithmetic operation to numbers.
     */
    private fun applyNumberOperation(
        oldValue: Number,
        newValue: Number,
        operation: MergeOperation
    ): Number {
        return when (operation) {
            MergeOperation.INCREMENT -> NumberOperationUtils.addNumbers(oldValue, newValue)
            MergeOperation.DECREMENT -> NumberOperationUtils.subtractNumbers(oldValue, newValue)
            else -> oldValue
        }
    }
}
