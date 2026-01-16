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
            operation.isNumericOperation() -> {
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
        // Skip GET and ARRAY_REMOVE operations on missing keys
        if (operation == ProfileOperation.GET || operation == ProfileOperation.ARRAY_REMOVE) {
            return
        }

        val updatedValue = when (operation) {
            ProfileOperation.DECREMENT -> {
                // For missing keys, DECREMENT means 0 - value = -value
                if (newValue !is Number) return
                NumberOperationUtils.negateNumber(newValue)
            }
            ProfileOperation.INCREMENT -> {
                // For missing keys, INCREMENT means 0 + value = value
                if (newValue !is Number) return
                newValue
            }
            else -> newValue
        }

        target.put(key, updatedValue)
        changeTracker.recordAddition(currentPath, updatedValue, changes)
    }

    /**
     * Handles arithmetic operations on numbers.
     */
    private fun handleNumberOperation(
        parent: JSONObject,
        key: String,
        oldValue: Any,
        newValue: Any,
        path: String,
        changes: MutableMap<String, ProfileChange>,
        operation: ProfileOperation
    ) {
        if (oldValue !is Number || newValue !is Number) {
            return
        }
        val result = when (operation) {
            ProfileOperation.INCREMENT -> NumberOperationUtils.addNumbers(oldValue, newValue)
            ProfileOperation.DECREMENT -> NumberOperationUtils.subtractNumbers(oldValue, newValue)
            else -> oldValue
        }

        if (!JsonComparisonUtils.areEqual(oldValue, result)) {
            parent.put(key, result)
            changeTracker.recordChange(path, oldValue, result, changes)
        }
    }

    /**
     * Handles simple value updates.
     * Recursively processes date prefixes in strings, arrays, and objects.
     */
    private fun handleValueUpdate(
        parent: JSONObject,
        key: String,
        oldValue: Any,
        newValue: Any,
        path: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        if (!JsonComparisonUtils.areEqual(oldValue, newValue)) {
            parent.put(key, newValue)
            changeTracker.recordChange(path, oldValue, newValue, changes)
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
        changes[path] = ProfileChange(oldValue, Constants.GET_MARKER)
    }
}
