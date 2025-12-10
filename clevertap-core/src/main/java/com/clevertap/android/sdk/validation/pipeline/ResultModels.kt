package com.clevertap.android.sdk.validation.pipeline

import org.json.JSONObject

/**
 * Result of event name normalization.
 */
data class EventNameNormalizationResult(
    val originalName: String?,
    val cleanedName: String?,
    val metrics: EventNameMetrics
)

/**
 * Metrics collected during event name normalization.
 */
data class EventNameMetrics(
    val originalLength: Int,
    val cleanedLength: Int,
    val modifications: Set<ModificationReason>
)

/**
 * Result of property key normalization.
 */
data class PropertyKeyNormalizationResult(
    val originalKey: String,
    val cleanedKey: String,
    val modifications: Set<KeyModification>,
    val wasRemoved: Boolean,
    val removalReason: RemovalReason?
)

/**
 * Result of event data normalization.
 */
data class EventDataNormalizationResult(
    val cleanedData: JSONObject,
    val metrics: EventDataMetrics
)

/**
 * Metrics collected during event data normalization.
 */
data class EventDataMetrics(
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

/**
 * Details about a key that was modified during normalization.
 */
data class KeyModification(
    val originalKey: String,
    val cleanedKey: String,
    val reasons: List<ModificationReason>
)

/**
 * Details about a value that was modified during normalization.
 */
data class ValueModification(
    val key: String,
    val originalValue: String,
    val cleanedValue: String,
    val reasons: List<ModificationReason>
)

/**
 * Details about an item that was removed during normalization.
 */
data class RemovedItem(
    val key: String,
    val reason: RemovalReason,
    val originalValue: Any?
)

/**
 * Common modification reason enum used across all normalizers.
 */
enum class ModificationReason {
    INVALID_CHARACTERS_REMOVED,
    TRUNCATED_TO_MAX_LENGTH
}

/**
 * Reasons why data was removed during normalization.
 */
enum class RemovalReason {
    NULL_VALUE,
    EMPTY_VALUE,
    EMPTY_KEY,
    NON_PRIMITIVE_VALUE,
    INVALID_PHONE_NUMBER,
    INVALID_COUNTRY_CODE
}
