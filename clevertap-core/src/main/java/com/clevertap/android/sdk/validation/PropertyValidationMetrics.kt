package com.clevertap.android.sdk.validation

/**
 * Detailed validation metrics collected during normalization.
 * Reports both original (before cleaning) and final (after cleaning) values.
 */
data class PropertyValidationMetrics(
    // Structural metrics
    val maxDepth: Int,
    val maxArrayKeyCount: Int,
    val maxObjectKeyCount: Int,
    val maxArrayLength: Int,
    val maxKVPairCount: Int,
    
    // Data quality metrics
    val keysModified: List<KeyModification>,
    val valuesModified: List<ValueModification>,
    val itemsRemoved: List<RemovedItem>
)

data class KeyModification(
    val originalKey: String,
    val cleanedKey: String,
    val reasons: List<KeyModificationReason>
)

data class ValueModification(
    val key: String,  // The key where this value is located
    val originalValue: String,
    val cleanedValue: String,
    val reasons: List<ValueModificationReason>
)

data class RemovedItem(
    val key: String,  // The key that was removed or had null/empty value
    val reason: RemovalReason,
    val originalValue: Any?
)

enum class KeyModificationReason {
    INVALID_CHARACTERS_REMOVED,
    TRUNCATED_TO_MAX_LENGTH
}

enum class ValueModificationReason {
    INVALID_CHARACTERS_REMOVED,
    TRUNCATED_TO_MAX_LENGTH
}

enum class RemovalReason {
    NULL_VALUE,
    EMPTY_VALUE,
    EMPTY_KEY,
    NON_PRIMITIVE_VALUE,
    INVALID_PHONE_NUMBER,
    INVALID_COUNTRY_CODE
}
