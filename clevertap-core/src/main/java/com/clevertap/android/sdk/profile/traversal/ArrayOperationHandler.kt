package com.clevertap.android.sdk.profile.traversal

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.min

/**
 * Handles array-specific operations: ARRAY_ADD, ARRAY_REMOVE, GET, UPDATE, and array element processing.
 * Supports both simple array operations and complex element-wise operations.
 */
internal class ArrayOperationHandler(private val changeTracker: ProfileChangeTracker) {

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
            ProfileOperation.ARRAY_REMOVE -> handleArrayRemove(
                parentJson,
                key,
                oldArray,
                newArray,
                currentPath,
                changes
            )

            ProfileOperation.GET -> getArrayElements(
                oldArray,
                newArray,
                currentPath,
                changes,
                recursiveTraversal
            )

            ProfileOperation.UPDATE -> handleArrayReplacement(
                parentJson,
                key,
                oldArray,
                newArray,
                currentPath,
                changes,
            )

            ProfileOperation.INCREMENT, ProfileOperation.DECREMENT -> processArrayElements(
                oldArray,
                newArray,
                currentPath,
                changes,
                operation,
                recursiveTraversal
            )

            else -> {}
        }
    }

    /**
     * Adds string values to array (allows duplicates).
     */
    private fun handleArrayAdd(
        oldArray: JSONArray,
        newArray: JSONArray,
        path: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        val oldArrayCopy = oldArray.deepCopy()
        var modified = false

        for (i in 0 until newArray.length()) {
            val item = newArray.get(i)
            if (item is String) {
                oldArray.put(item)
                modified = true
            }
        }

        if (modified) {
            changeTracker.recordChange(path, oldArrayCopy, oldArray, changes)
        }
    }

    /**
     * Replaces the entire array with new array for UPDATE operation.
     */
    private fun handleArrayReplacement(
        parentJson: JSONObject,
        key: String,
        oldArray: JSONArray,
        newArray: JSONArray,
        path: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        // Only record change if arrays are different
        if (!JsonComparisonUtils.areEqual(oldArray, newArray)) {
            parentJson.put(key, newArray)
            changeTracker.recordChange(path, oldArray, newArray, changes)
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
        val oldArrayCopy = oldArray.deepCopy()
        val resultArray = JSONArray()
        var modified = false

        for (i in 0 until oldArray.length()) {
            val item = oldArray.get(i)
            if (item is String && newArray.containsString(item)) {
                modified = true
            } else {
                resultArray.put(item)
            }
        }

        if (modified) {
            parentJson.put(key, resultArray)
            changeTracker.recordChange(path, oldArrayCopy, resultArray, changes)
        }
    }

    /**
     * Processes array elements individually for INCREMENT/DECREMENT operations.
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
        val oldArrayCopy = oldArray.deepCopy()
        var arrayModified = false

        val arrayLength = min(oldArray.length(), newArray.length())

        for (i in 0 until arrayLength) {
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
            }
        }

        if (arrayModified) {
            changeTracker.recordChange(basePath, oldArrayCopy, oldArray, changes)
        }
    }

    /**
     * Applies arithmetic operation to numbers.
     * Only called for INCREMENT/DECREMENT operations.
     */
    private fun applyNumberOperation(
        oldValue: Number,
        newValue: Number,
        operation: ProfileOperation
    ): Number {
        return if (operation == ProfileOperation.INCREMENT) {
            NumberOperationUtils.addNumbers(oldValue, newValue)
        } else {
            NumberOperationUtils.subtractNumbers(oldValue, newValue)
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
