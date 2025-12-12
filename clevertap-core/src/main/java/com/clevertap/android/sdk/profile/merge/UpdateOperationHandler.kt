package com.clevertap.android.sdk.profile.merge

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Handles UPDATE, INCREMENT, and DECREMENT operations during profile merging.
 * Manages value updates, missing keys, and number operations.
 */
internal class UpdateOperationHandler(
    private val changeTracker: ProfileChangeTracker,
    private val arrayHandler: ArrayOperationHandler
) {

    /**
     * Handles non-delete merge operations for a key in the target object.
     *
     * @param target The JSON object to merge into
     * @param key The key to process
     * @param newValue The value from source
     * @param currentPath The dot-notation path
     * @param changes The map to accumulate changes in
     * @param operation The merge operation type
     * @param recursiveMerge Function to recursively merge nested objects
     */
    @Throws(JSONException::class)
    fun handleMerge(
        target: JSONObject,
        key: String,
        newValue: Any,
        currentPath: String,
        changes: MutableMap<String, ProfileChange>,
        operation: MergeOperation,
        recursiveMerge: (JSONObject, JSONObject?, String, MutableMap<String, ProfileChange>) -> Unit
    ) {
        if (!target.has(key)) {
            handleMissingKey(target, key, newValue, currentPath, changes, operation)
            return
        }

        val oldValue = target.get(key)

        // Special handling for GET operation
        if (operation == MergeOperation.GET) {
            when {
                oldValue is JSONObject && newValue is JSONObject -> {
                    // Recurse into nested objects for GET
                    recursiveMerge(oldValue, newValue, currentPath, changes)
                }
                oldValue is JSONArray && newValue is JSONArray -> {
                    // Handle array GET operations
                    arrayHandler.handleArrayMerge(
                        target, key, oldValue, newValue, currentPath, changes, operation, recursiveMerge
                    )
                }
                else -> {
                    // Found the target value - report it without updating
                    handleGetOperation(oldValue, currentPath, changes)
                }
            }
            return
        }

        when {
            oldValue is JSONObject && newValue is JSONObject -> {
                // Recurse into nested objects
                recursiveMerge(oldValue, newValue, currentPath, changes)
            }
            oldValue is JSONArray && newValue is JSONArray -> {
                // Handle array operations
                arrayHandler.handleArrayMerge(
                    target, key, oldValue, newValue, currentPath, changes, operation, recursiveMerge
                )
            }
            oldValue is Number && newValue is Number &&
                    operation in listOf(MergeOperation.INCREMENT, MergeOperation.DECREMENT) -> {
                // Handle arithmetic operations
                handleNumberOperation(target, key, oldValue, newValue, currentPath, changes, operation)
            }
            else -> {
                // Handle simple value update
                handleValueUpdate(target, key, oldValue, newValue, currentPath, changes)
            }
        }
    }

    /**
     * Handles keys that don't exist in the target.
     * Skips arithmetic operations and GET operations on missing keys.
     */
    private fun handleMissingKey(
        target: JSONObject,
        key: String,
        newValue: Any,
        currentPath: String,
        changes: MutableMap<String, ProfileChange>,
        operation: MergeOperation
    ) {
        // Skip adding keys for arithmetic operations and GET operations
        if (operation in listOf(MergeOperation.INCREMENT, MergeOperation.DECREMENT, MergeOperation.GET)) {
            return
        }

        target.put(key, newValue)

        if (newValue is JSONObject) {
            changeTracker.recordAllLeafValues(newValue, currentPath, changes)
        } else {
            changes[currentPath] = ProfileChange(null, newValue)
        }
    }

    /**
     * Handles arithmetic operations on numbers.
     */
    private fun handleNumberOperation(
        parent: JSONObject,
        key: String,
        oldValue: Number,
        newValue: Number,
        path: String,
        changes: MutableMap<String, ProfileChange>,
        operation: MergeOperation
    ) {
        val result = when (operation) {
            MergeOperation.INCREMENT -> NumberOperationUtils.addNumbers(oldValue, newValue)
            MergeOperation.DECREMENT -> NumberOperationUtils.subtractNumbers(oldValue, newValue)
            else -> oldValue
        }

        if (!JsonComparisonUtils.areEqual(oldValue, result)) {
            parent.put(key, result)
            changes[path] = ProfileChange(oldValue, result)
        }
    }

    /**
     * Handles simple value updates.
     * If new value is a string with $D_ prefix, removes it and converts to long.
     */
    private fun handleValueUpdate(
        parent: JSONObject,
        key: String,
        oldValue: Any,
        newValue: Any,
        path: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        val processedOldValue = if (oldValue is String) {
            ProfileMergeConstants.processDatePrefix(oldValue)
        } else {
            oldValue
        }

        val processedNewValue = if (newValue is String) {
            ProfileMergeConstants.processDatePrefix(newValue)
        } else {
            newValue
        }

        if (!JsonComparisonUtils.areEqual(processedOldValue, processedNewValue)) {
            parent.put(key, processedNewValue)
            changes[path] = ProfileChange(processedOldValue, processedNewValue)
        }
    }

    /**
     * Handles GET operation - reports the current value without modifying it.
     * Records a ProfileChange with oldValue set to the current value and newValue set to "__GET_MARKER__".
     */
    private fun handleGetOperation(
        oldValue: Any,
        path: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        val processedOldValue = if (oldValue is String) {
            ProfileMergeConstants.processDatePrefix(oldValue)
        } else {
            oldValue
        }
        
        changes[path] = ProfileChange(processedOldValue, "__GET_MARKER__")
    }
}
