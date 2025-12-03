package com.clevertap.android.sdk.profile

import androidx.annotation.WorkerThread
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * ProfileStateMerger provides functionality to merge JSON profile data with various operations.
 * Supports nested objects, array operations, and tracks all changes with dot notation paths.
 *
 * This class is designed for merging user profile updates into existing profile state,
 * supporting operations like UPDATE, INCREMENT, DECREMENT, DELETE, and array modifications.
 *
 * Thread Safety: This class is NOT thread-safe. Callers must ensure proper synchronization
 * when accessing the same JSONObject from multiple threads.
 *
 * @see MergeOperation for available operations
 * @see ProfileChange for tracking changes
 */
// todo check for JSONObject.of
// todo changes for $D_ should be reported as without $D
class ProfileStateMerger {

    /**
     * Defines the type of merge operation to perform on profile data.
     */
    enum class MergeOperation {
        /**
         * Replace old values with new values. Creates keys if they don't exist.
         * For nested objects, recursively merges properties.
         */
        UPDATE,

        /**
         * Add new value to old value (numeric values only).
         * Skips non-numeric values and keys that don't exist.
         */
        INCREMENT,

        /**
         * Subtract new value from old value (numeric values only).
         * Skips non-numeric values and keys that don't exist.
         */
        DECREMENT,

        /**
         * Delete keys specified in source from target.
         * Use DELETE_MARKER constant as value to mark fields for deletion.
         */
        DELETE,

        /**
         * Add string values to arrays.
         * Creates array if it doesn't exist. Skips duplicates.
         * Only works with string array elements.
         */
        ARRAY_ADD,

        /**
         * Remove string values from arrays.
         * Only works with string array elements.
         */
        ARRAY_REMOVE
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
    /**
     * Result of a merge operation containing all changes made.
     *
     * @property changes Map of dot-notation paths to ProfileChange objects
     */
    data class MergeResult(
        val changes: Map<String, ProfileChange>,
    )

    /**
     * Merges source JSON into target JSON and returns all changes.
     *
     * @param target The JSON object to merge into (modified in place)
     * @param source The JSON object to merge from (not modified)
     * @param operation The merge operation to perform
     * @return MergeResult containing all changes with dot-notation paths
     * @throws org.json.JSONException if there's an error during merge
     */
    @WorkerThread
    @Throws(JSONException::class)
    fun merge(
        target: JSONObject,
        source: JSONObject,
        operation: MergeOperation = MergeOperation.UPDATE
    ): MergeResult {
        val changes = mutableMapOf<String, ProfileChange>()
        mergeRecursive(target, source, "", changes, operation)
        return MergeResult(changes)
    }

    /**
     * Recursively merges source into target, tracking changes at each level.
     *
     * @param target The JSON object being merged into
     * @param source The JSON object being merged from
     * @param path Current dot-notation path (for tracking)
     * @param changes Accumulator for all changes
     * @param operation The merge operation to perform
     */
    @Throws(JSONException::class)
    private fun mergeRecursive(
        target: JSONObject,
        source: JSONObject?,
        path: String,
        changes: MutableMap<String, ProfileChange>,
        operation: MergeOperation
    ) {
        if (source == null) return

        val keys = source.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val newValue = source.get(key)
            val currentPath = buildPath(path, key)

            when (operation) {
                MergeOperation.DELETE -> handleDelete(target, key, newValue, currentPath, changes)
                else -> handleMerge(target, key, newValue, currentPath, changes, operation)
            }
        }
    }

    /**
     * Handles deletion operations.
     */
    private fun handleDelete(
        target: JSONObject,
        key: String,
        newValue: Any,
        currentPath: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        if (!target.has(key)) return

        val oldValue = target.get(key)

        when {
            isDeleteMarker(newValue) -> {
                // Delete this key entirely
                deleteValue(target, key, oldValue, currentPath, changes)
            }
            oldValue is JSONObject && newValue is JSONObject -> {
                // Recurse into nested objects
                mergeRecursive(oldValue, newValue, currentPath, changes, MergeOperation.DELETE)
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

        val hasDeleteMarkers = hasDeleteMarkerElements(newArray)
        val hasObjectsToDelete = hasJsonObjectElements(newArray)

        when {
            hasDeleteMarkers -> deleteArrayElements(oldArray, newArray, currentPath, changes)
            hasObjectsToDelete -> deleteFromArrayElements(oldArray, newArray, currentPath, changes)
            else -> deleteValue(parentJson, key, oldArray, currentPath, changes)
        }
    }

    /**
     * Deletes specific fields from array elements.
     */
    private fun deleteFromArrayElements(
        oldArray: JSONArray,
        newArray: JSONArray,
        basePath: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        val oldArrayCopy = copyArray(oldArray)
        var arrayModified = false

        for (i in 0 until minOf(newArray.length(), oldArray.length())) {
            val oldElement = oldArray.get(i)
            val newElement = newArray.get(i)

            if (oldElement is JSONObject && newElement is JSONObject) {
                mergeRecursive(oldElement, newElement, "", mutableMapOf(), MergeOperation.DELETE)
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
     * Deletes entire array elements at specific indices marked with DELETE_MARKER.
     */
    private fun deleteArrayElements(
        oldArray: JSONArray,
        newArray: JSONArray,
        basePath: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        val oldArrayCopy = copyArray(oldArray)
        val indicesToDelete = mutableListOf<Int>()

        // Collect indices to delete
        for (i in 0 until newArray.length()) {
            if (isDeleteMarker(newArray.opt(i)) && i < oldArray.length()) {
                indicesToDelete.add(i)
            }
        }

        if (indicesToDelete.isEmpty()) return

        // Delete in reverse order to maintain correct indices
        indicesToDelete.sortedDescending().forEach { index ->
            oldArray.remove(index)
        }

        changes[basePath] = ProfileChange(oldArrayCopy, oldArray)
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
        if (value is JSONObject) {
            recordAllLeafDeletions(value, path, changes)
        } else {
            changes[path] = ProfileChange(value, null)
        }
        parent.remove(key)
    }

    /**
     * Handles non-delete merge operations.
     */
    private fun handleMerge(
        target: JSONObject,
        key: String,
        newValue: Any,
        currentPath: String,
        changes: MutableMap<String, ProfileChange>,
        operation: MergeOperation
    ) {
        if (!target.has(key)) {
            handleMissingKey(target, key, newValue, currentPath, changes, operation)
            return
        }

        val oldValue = target.get(key)

        when {
            oldValue is JSONObject && newValue is JSONObject -> {
                // Recurse into nested objects
                mergeRecursive(oldValue, newValue, currentPath, changes, operation)
            }
            oldValue is JSONArray && newValue is JSONArray -> {
                // Handle array operations
                handleArrayMerge(target, key, oldValue, newValue, currentPath, changes, operation)
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
     */
    private fun handleMissingKey(
        target: JSONObject,
        key: String,
        newValue: Any,
        currentPath: String,
        changes: MutableMap<String, ProfileChange>,
        operation: MergeOperation
    ) {
        // Skip adding keys for arithmetic operations
        if (operation in listOf(MergeOperation.INCREMENT, MergeOperation.DECREMENT)) {
            return
        }

        target.put(key, newValue)

        if (newValue is JSONObject) {
            recordAllLeafValues(newValue, currentPath, changes)
        } else {
            changes[currentPath] = ProfileChange(null, newValue)
        }
    }

    /**
     * Handles array merge operations.
     */
    private fun handleArrayMerge(
        parentJson: JSONObject,
        key: String,
        oldArray: JSONArray,
        newArray: JSONArray,
        currentPath: String,
        changes: MutableMap<String, ProfileChange>,
        operation: MergeOperation
    ) {
        if (newArray.length() == 0) return

        when (operation) {
            MergeOperation.ARRAY_ADD -> handleArrayAdd(oldArray, newArray, currentPath, changes)
            MergeOperation.ARRAY_REMOVE -> handleArrayRemove(parentJson, key, oldArray, newArray, currentPath, changes)
            MergeOperation.UPDATE, MergeOperation.INCREMENT, MergeOperation.DECREMENT -> {
                if (shouldMergeArrayElements(newArray)) {
                    mergeArrayElements(oldArray, newArray, currentPath, changes, operation)
                } else {
                    handleArrayReplacement(parentJson, key, oldArray, newArray, currentPath, changes)
                }
            }
            else -> {}
        }
    }

    /**
     * Determines if array should be merged element-wise or replaced.
     */
    private fun shouldMergeArrayElements(array: JSONArray): Boolean {
        return (0 until array.length()).any { i -> array.opt(i) is JSONObject }
    }

    /**
     * Merges array elements individually.
     */
    private fun mergeArrayElements(
        oldArray: JSONArray,
        newArray: JSONArray,
        basePath: String,
        changes: MutableMap<String, ProfileChange>,
        operation: MergeOperation
    ) {
        val oldArrayCopy = copyArray(oldArray)
        var arrayModified = false

        for (i in 0 until newArray.length()) {
            if (i >= oldArray.length()) {
                handleOutOfBoundsIndex(oldArray, newArray, i, operation)
                arrayModified = true
                continue
            }

            val oldElement = oldArray.get(i)
            val newElement = newArray.get(i)

            when {
                oldElement is JSONObject && newElement is JSONObject -> {
                    val elementChanges = mutableMapOf<String, ProfileChange>()
                    mergeRecursive(oldElement, newElement, "", elementChanges, operation)
                    if (elementChanges.isNotEmpty()) {
                        arrayModified = true
                    }
                }
                oldElement is Number && newElement is Number -> {
                    val result = applyNumberOperation(oldElement, newElement, operation)
                    if (!areEqual(oldElement, result)) {
                        oldArray.put(i, result)
                        arrayModified = true
                    }
                }
                operation == MergeOperation.UPDATE && !areEqual(oldElement, newElement) -> {
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
     */
    private fun handleOutOfBoundsIndex(
        oldArray: JSONArray,
        newArray: JSONArray,
        index: Int,
        operation: MergeOperation
    ) {
        if (operation != MergeOperation.UPDATE) return

        val newElement = newArray.get(index)
        while (oldArray.length() <= index) {
            oldArray.put(JSONObject.NULL)
        }
        oldArray.put(index, newElement)
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
        val oldArrayCopy = copyArray(oldArray)
        var modified = false

        for (i in 0 until newArray.length()) {
            val item = newArray.get(i)
            if (item is String) {
                oldArray.put(item)
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
        val oldArrayCopy = copyArray(oldArray)
        val resultArray = JSONArray()
        var modified = false

        for (i in 0 until oldArray.length()) {
            val item = oldArray.get(i)
            if (item is String && arrayContainsString(newArray, item)) {
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
        if (!areEqual(oldArray, newArray)) {
            parentJson.put(key, newArray)
            changes[path] = ProfileChange(oldArray, newArray)
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
        val result = applyNumberOperation(oldValue, newValue, operation)

        if (!areEqual(oldValue, result)) {
            parent.put(key, result)
            changes[path] = ProfileChange(oldValue, result)
        }
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
            MergeOperation.INCREMENT -> addNumbers(oldValue, newValue)
            MergeOperation.DECREMENT -> subtractNumbers(oldValue, newValue)
            else -> oldValue
        }
    }

    /**
     * Handles simple value updates.
     */
    private fun handleValueUpdate(
        parent: JSONObject,
        key: String,
        oldValue: Any,
        newValue: Any,
        path: String,
        changes: MutableMap<String, ProfileChange>
    ) {
        if (!areEqual(oldValue, newValue)) {
            parent.put(key, newValue)
            changes[path] = ProfileChange(oldValue, newValue)
        }
    }

    /**
     * Records all leaf values in a JSONObject as additions.
     */
    private fun recordAllLeafValues(
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
     * Records all leaf values in a JSONObject as deletions.
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
                else -> changes[currentPath] = ProfileChange(value, null)
            }
        }
    }

    // ==================== Utility Methods ====================

    /**
     * Builds a dot-notation path from base path and key.
     */
    private fun buildPath(basePath: String, key: String): String {
        return if (basePath.isEmpty()) key else "$basePath.$key"
    }

    /**
     * Adds two numbers, preserving type when possible.
     */
    private fun addNumbers(a: Number, b: Number): Number {
        return when {
            a is Int && b is Int -> a + b
            a is Long || b is Long -> a.toLong() + b.toLong()
            a is Float || b is Float -> a.toFloat() + b.toFloat()
            else -> a.toDouble() + b.toDouble()
        }
    }

    /**
     * Subtracts two numbers, preserving type when possible.
     */
    private fun subtractNumbers(a: Number, b: Number): Number {
        return when {
            a is Int && b is Int -> a - b
            a is Long || b is Long -> a.toLong() - b.toLong()
            a is Float || b is Float -> a.toFloat() - b.toFloat()
            else -> a.toDouble() - b.toDouble()
        }
    }

    /**
     * Deep equality check for JSON values.
     */
    private fun areEqual(obj1: Any?, obj2: Any?): Boolean {
        if (obj1 == null && obj2 == null) return true
        if (obj1 == null || obj2 == null) return false
        if (obj1 === obj2) return true

        return when {
            obj1 is JSONObject && obj2 is JSONObject -> jsonObjectsEqual(obj1, obj2)
            obj1 is JSONArray && obj2 is JSONArray -> jsonArraysEqual(obj1, obj2)
            else -> obj1 == obj2
        }
    }

    /**
     * Checks if two JSONObjects are equal.
     */
    private fun jsonObjectsEqual(obj1: JSONObject, obj2: JSONObject): Boolean {
        if (obj1.length() != obj2.length()) return false

        val keys1 = obj1.keys()
        while (keys1.hasNext()) {
            val key = keys1.next()
            if (!obj2.has(key)) return false

            val value1 = obj1.get(key)
            val value2 = obj2.get(key)

            if (!areEqual(value1, value2)) return false
        }

        return true
    }

    /**
     * Checks if two JSONArrays are equal.
     */
    private fun jsonArraysEqual(arr1: JSONArray, arr2: JSONArray): Boolean {
        if (arr1.length() != arr2.length()) return false

        for (i in 0 until arr1.length()) {
            val value1 = arr1.get(i)
            val value2 = arr2.get(i)

            if (!areEqual(value1, value2)) return false
        }

        return true
    }

    /**
     * Checks if array contains a specific string.
     */
    private fun arrayContainsString(array: JSONArray, value: String): Boolean {
        for (i in 0 until array.length()) {
            val item = array.get(i)
            if (item is String && item == value) {
                return true
            }
        }
        return false
    }

    /**
     * Creates a deep copy of a JSONArray.
     */
    private fun copyArray(array: JSONArray): JSONArray {
        return JSONArray(array.toString())
    }

    /**
     * Checks if array has elements marked for deletion.
     */
    private fun hasDeleteMarkerElements(array: JSONArray): Boolean {
        return (0 until array.length()).any { i -> isDeleteMarker(array.opt(i)) }
    }

    /**
     * Checks if array has JSONObject elements.
     */
    private fun hasJsonObjectElements(array: JSONArray): Boolean {
        return (0 until array.length()).any { i ->
            val element = array.opt(i)
            element is JSONObject && !isDeleteMarker(element)
        }
    }

    companion object {
        /**
         * Special marker to indicate deletion intent.
         * Use this value to mark fields for deletion.
         */
        const val DELETE_MARKER = "__CLEVERTAP_DELETE__"

        /**
         * Checks if a value is the DELETE_MARKER.
         */
        @JvmStatic
        fun isDeleteMarker(value: Any?): Boolean {
            return value is String && value == DELETE_MARKER
        }

        /**
         * Extension function to convert ProfileChange map to nested map format.
         * Useful for serialization or legacy compatibility.
         */
        @JvmStatic
        fun Map<String, ProfileChange>.toNestedMap(): Map<String, Map<String, Any?>> {
            return this.mapValues { (_, change) ->
                mapOf(
                    "oldValue" to change.oldValue,
                    "newValue" to change.newValue
                )
            }
        }
    }
}