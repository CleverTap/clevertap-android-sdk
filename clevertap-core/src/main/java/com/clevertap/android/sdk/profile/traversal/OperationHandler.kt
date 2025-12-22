package com.clevertap.android.sdk.profile.traversal

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Handles UPDATE, INCREMENT, DECREMENT, and GET operations during profile processing.
 * Manages value updates, retrieval, missing keys, and number operations.
 */
internal class OperationHandler(
    private val changeTracker: ProfileChangeTracker,
    private val arrayHandler: ArrayOperationHandler
) {

    /**
     * Handles non-delete operations for a key in the target object.
     *
     * @param target The JSON object to operate on
     * @param key The key to process
     * @param newValue The value from source
     * @param currentPath The dot-notation path
     * @param changes The map to accumulate changes in
     * @param operation The profile operation type
     * @param recursiveApply Function to recursively apply operation to nested objects
     */
    @Throws(JSONException::class)
    fun handleOperation(
        target: JSONObject,
        key: String,
        newValue: Any,
        currentPath: String,
        changes: MutableMap<String, ProfileChange>,
        operation: ProfileOperation,
        recursiveApply: (JSONObject, JSONObject?, String, MutableMap<String, ProfileChange>) -> Unit
    ) {
        if (!target.has(key)) {
            handleMissingKey(target, key, newValue, currentPath, changes, operation)
            return
        }

        val oldValue = target.get(key)

        when {
            oldValue is JSONObject && newValue is JSONObject -> {
                // Recurse into nested objects (works for both GET and other operations)
                recursiveApply(oldValue, newValue, currentPath, changes)
            }
            oldValue is JSONArray && newValue is JSONArray -> {
                arrayHandler.handleArrayOperation(
                    target, key, oldValue, newValue, currentPath, changes, operation, recursiveApply
                )
            }
            oldValue is Number && newValue is Number &&
                    operation in listOf(ProfileOperation.INCREMENT, ProfileOperation.DECREMENT) -> {
                handleNumberOperation(target, key, oldValue, newValue, currentPath, changes, operation)
            }
            operation == ProfileOperation.GET -> {
                handleGetOperation(oldValue, currentPath, changes)
            }
            else -> {
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
        operation: ProfileOperation
    ) {
        // Skip adding keys for arithmetic operations and GET operations
        if (operation in listOf(ProfileOperation.INCREMENT, ProfileOperation.DECREMENT, ProfileOperation.GET)) {
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
        operation: ProfileOperation
    ) {
        val result = when (operation) {
            ProfileOperation.INCREMENT -> NumberOperationUtils.addNumbers(oldValue, newValue)
            ProfileOperation.DECREMENT -> NumberOperationUtils.subtractNumbers(oldValue, newValue)
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
            ProfileOperationUtils.processDatePrefix(oldValue)
        } else {
            oldValue
        }

        val processedNewValue = if (newValue is String) {
            ProfileOperationUtils.processDatePrefix(newValue)
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
     * Records a ProfileChange with oldValue set to the current value and newValue set to "__CLEVERTAP_GET__".
     */
    private fun handleGetOperation(
        oldValue: Any,
        path: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        val processedOldValue = if (oldValue is String) {
            ProfileOperationUtils.processDatePrefix(oldValue)
        } else {
            oldValue
        }
        
        changes[path] = ProfileChange(processedOldValue, Constants.GET_MARKER)
    }
}
