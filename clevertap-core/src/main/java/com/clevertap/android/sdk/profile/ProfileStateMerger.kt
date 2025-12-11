package com.clevertap.android.sdk.profile

import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.profile.merge.ArrayOperationHandler
import com.clevertap.android.sdk.profile.merge.DeleteOperationHandler
import com.clevertap.android.sdk.profile.merge.MergeOperation
import com.clevertap.android.sdk.profile.merge.ProfileChange
import com.clevertap.android.sdk.profile.merge.ProfileChangeTracker
import com.clevertap.android.sdk.profile.merge.UpdateOperationHandler
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
class ProfileStateMerger {

    private val changeTracker = ProfileChangeTracker()
    private val arrayHandler = ArrayOperationHandler()
    private val updateHandler = UpdateOperationHandler(changeTracker, arrayHandler)
    private val deleteHandler = DeleteOperationHandler(changeTracker)

    /**
     * Result of a merge operation containing all changes made.
     *
     * @property changes Map of dot-notation paths to ProfileChange objects
     */
    data class MergeResult(
        val changes: Map<String, ProfileChange>
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
                MergeOperation.DELETE -> {
                    deleteHandler.handleDelete(
                        target, key, newValue, currentPath, changes
                    ) { target, source, path, changes ->
                        mergeRecursive(target, source, path, changes, MergeOperation.DELETE)
                    }
                }
                else -> {
                    updateHandler.handleMerge(
                        target, key, newValue, currentPath, changes, operation
                    ) { target, source, path, changes ->
                        mergeRecursive(target, source, path, changes, operation)
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
