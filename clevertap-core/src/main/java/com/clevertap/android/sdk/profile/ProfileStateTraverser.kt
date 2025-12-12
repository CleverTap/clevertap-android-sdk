package com.clevertap.android.sdk.profile

import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.profile.traversal.ArrayOperationHandler
import com.clevertap.android.sdk.profile.traversal.DeleteOperationHandler
import com.clevertap.android.sdk.profile.traversal.ProfileOperation
import com.clevertap.android.sdk.profile.traversal.ProfileChange
import com.clevertap.android.sdk.profile.traversal.ProfileChangeTracker
import com.clevertap.android.sdk.profile.traversal.UpdateOperationHandler
import org.json.JSONException
import org.json.JSONObject

/**
 * ProfileStateTraverser provides functionality to merge JSON profile data with various operations.
 * Supports nested objects, array operations, and tracks all changes with dot notation paths.
 *
 * This class is designed for applying operations to user profile state,
 * supporting operations like UPDATE, INCREMENT, DECREMENT, DELETE, GET, and array modifications.
 *
 * Thread Safety: This class is NOT thread-safe. Callers must ensure proper synchronization
 * when accessing the same JSONObject from multiple threads.
 *
 * @see ProfileOperation for available operations
 * @see ProfileChange for tracking changes
 */
class ProfileStateTraverser {

    private val changeTracker = ProfileChangeTracker()
    private val arrayHandler = ArrayOperationHandler()
    private val updateHandler = UpdateOperationHandler(changeTracker, arrayHandler)
    private val deleteHandler = DeleteOperationHandler(changeTracker)

    /**
     * Result of a merge operation containing all changes made.
     *
     * @property changes Map of dot-notation paths to ProfileChange objects
     */
    data class ProfileTraversalResult(
        val changes: Map<String, ProfileChange>
    )

    /**
     * Applies an operation to profile data and returns all changes.
     *
     * @param target The JSON object to operate on (modified in place for non-GET operations)
     * @param source The JSON object containing operation parameters (not modified)
     * @param operation The profile operation to perform
     * @return ProfileTraversalResult containing all changes with dot-notation paths
     * @throws org.json.JSONException if there's an error during operation
     */
    @WorkerThread
    @Throws(JSONException::class)
    fun traverse(
        target: JSONObject,
        source: JSONObject,
        operation: ProfileOperation = ProfileOperation.UPDATE
    ): ProfileTraversalResult {
        val changes = mutableMapOf<String, ProfileChange>()
        traverseRecursive(target, source, "", changes, operation)
        return ProfileTraversalResult(changes)
    }

    /**
     * Recursively applies operation to nested structures, tracking changes at each level.
     *
     * @param target The JSON object being operated on
     * @param source The JSON object containing operation parameters
     * @param path Current dot-notation path (for tracking)
     * @param changes Accumulator for all changes
     * @param operation The profile operation to perform
     */
    @Throws(JSONException::class)
    private fun traverseRecursive(
        target: JSONObject,
        source: JSONObject?,
        path: String,
        changes: MutableMap<String, ProfileChange>,
        operation: ProfileOperation
    ) {
        if (source == null) return

        val keys = source.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val newValue = source.get(key)
            val currentPath = buildPath(path, key)

            when (operation) {
                ProfileOperation.DELETE -> {
                    deleteHandler.handleDelete(
                        target, key, newValue, currentPath, changes
                    ) { target, source, path, changes ->
                        traverseRecursive(target, source, path, changes, ProfileOperation.DELETE)
                    }
                }
                else -> {
                    updateHandler.handleOperation(
                        target, key, newValue, currentPath, changes, operation
                    ) { target, source, path, changes ->
                        traverseRecursive(target, source, path, changes, operation)
                    }
                }
            }
        }
    }

    /**
     * Builds a dot-notation path from base path and key.
     */
    private fun buildPath(basePath: String, key: String): String {
        return if (basePath.isEmpty()) key else "$basePath.$key"
    }

    companion object {
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
