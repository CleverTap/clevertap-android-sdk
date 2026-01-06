package com.clevertap.android.sdk.profile.traversal

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Handles array-specific operations: ARRAY_ADD, ARRAY_REMOVE, GET, and array element processing.
 * Supports both simple array operations and complex element-wise operations.
 */
internal class ArrayOperationHandler() {

    /**
     * Handles all array operations based on the operation type.
     *
     * @param parentJson The parent JSON object containing the array
     * @param key The key of the array in the parent
     * @param oldArray The existing array
     * @param newArray The array with operation parameters
     * @param currentPath The dot-notation path
     * @param changes The map to accumulate changes in
     * @param operation The profile operation type
     * @param recursiveTraversal Function to recursively apply operation to nested objects
     */
    @Throws(JSONException::class)
    fun handleArrayOperation(
        parentJson: JSONObject,
        key: String,
        oldArray: JSONArray,
        newArray: JSONArray,
        currentPath: String,
        changes: MutableMap<String, ProfileChange>,
        operation: ProfileOperation,
        recursiveTraversal: (JSONObject, JSONObject?, String, MutableMap<String, ProfileChange>) -> Unit
    ) {
        if (newArray.length() == 0) return

        when (operation) {
            ProfileOperation.ARRAY_ADD -> handleArrayAdd(oldArray, newArray, currentPath, changes)
            ProfileOperation.ARRAY_REMOVE -> handleArrayRemove(parentJson, key, oldArray, newArray, currentPath, changes)
            ProfileOperation.GET -> {
                getArrayElements(oldArray, newArray, currentPath, changes, recursiveTraversal)
            }
            ProfileOperation.UPDATE, ProfileOperation.INCREMENT, ProfileOperation.DECREMENT -> {
                if (ArrayMergeUtils.shouldMergeArrayElements(newArray)) {
                    processArrayElements(oldArray, newArray, currentPath, changes, operation, recursiveTraversal)
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
                val processedItem = ProfileOperationUtils.processDatePrefixes(item)
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
     * Processes date prefixes for comparison, but stores original value.
     */
    private fun handleArrayReplacement(
        parentJson: JSONObject,
        key: String,
        oldArray: JSONArray,
        newArray: JSONArray,
        path: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        val processedOldArray = ProfileOperationUtils.processDatePrefixes(oldArray)
        val processedNewArray = ProfileOperationUtils.processDatePrefixes(newArray)
        
        if (!JsonComparisonUtils.areEqual(processedOldArray, processedNewArray)) {
            parentJson.put(key, newArray)  // Store original value with prefix
            changes[path] = ProfileChange(processedOldArray, processedNewArray)  // Track processed values
        }
    }

    /**
     * Processes array elements individually.
     * Handles objects, numbers, and simple values differently based on operation.
     */
    private fun processArrayElements(
        oldArray: JSONArray,
        newArray: JSONArray,
        basePath: String,
        changes: MutableMap<String, ProfileChange>,
        operation: ProfileOperation,
        recursiveTraversal: (JSONObject, JSONObject?, String, MutableMap<String, ProfileChange>) -> Unit
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
                    recursiveTraversal(oldElement, newElement, "", elementChanges)
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
                operation == ProfileOperation.UPDATE -> {
                    val processedOldElement = ProfileOperationUtils.processDatePrefixes(oldElement)
                    val processedNewElement = ProfileOperationUtils.processDatePrefixes(newElement)
                    if (!JsonComparisonUtils.areEqual(processedOldElement, processedNewElement)) {
                        oldArray.put(i, newElement)  // Store original value with prefix
                        arrayModified = true
                    }
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
        operation: ProfileOperation
    ): Boolean {
        if (operation != ProfileOperation.UPDATE) return false

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
        operation: ProfileOperation
    ): Number {
        return when (operation) {
            ProfileOperation.INCREMENT -> NumberOperationUtils.addNumbers(oldValue, newValue)
            ProfileOperation.DECREMENT -> NumberOperationUtils.subtractNumbers(oldValue, newValue)
            else -> oldValue
        }
    }

    /**
     * Gets array elements for GET operation without modifying the array.
     * Reports each accessed element with "__CLEVERTAP_GET__" as the new value.
     */
    private fun getArrayElements(
        oldArray: JSONArray,
        newArray: JSONArray,
        basePath: String,
        changes: MutableMap<String, ProfileChange>,
        recursiveTraversal: (JSONObject, JSONObject?, String, MutableMap<String, ProfileChange>) -> Unit
    ) {
        for (i in 0 until newArray.length()) {
            if (i >= oldArray.length()) {
                // Index out of bounds - skip
                continue
            }

            val oldElement = oldArray.get(i)
            val newElement = newArray.get(i)
            val elementPath = "$basePath[$i]"

            when {
                oldElement is JSONObject && newElement is JSONObject -> {
                    // Recurse into nested objects
                    recursiveTraversal(oldElement, newElement, elementPath, changes)
                }
                else -> {
                    // Report the element value without modification
                    changes[elementPath] = ProfileChange(oldElement, Constants.GET_MARKER)
                }
            }
        }
    }
}
