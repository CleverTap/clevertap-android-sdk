package com.clevertap.android.sdk.profile.merge

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
    ARRAY_REMOVE,

    GET
}
