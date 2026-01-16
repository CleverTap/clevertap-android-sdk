package com.clevertap.android.sdk.profile.traversal

/**
 * Defines the type of operation to perform on profile data.
 */
enum class ProfileOperation {
    /**
     * Replace old values with new values. Creates keys if they don't exist.
     * For nested objects, recursively updates properties.
     */
    UPDATE,

    /**
     * Add new value to old value (numeric values only).
     * Skips non-numeric values
     * Adds the value if key doesn't exist
     */
    INCREMENT,

    /**
     * Subtract new value from old value (numeric values only).
     * Skips non-numeric values
     * Adds the negated value if key doesn't exist
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

    /**
     * Retrieve values without modification.
     * Navigates through nested objects and arrays to report existing values.
     * Changes are reported with newValue = "__CLEVERTAP_GET__".
     */
    GET;

    fun isNumericOperation(): Boolean =
        this == INCREMENT || this == DECREMENT
}
