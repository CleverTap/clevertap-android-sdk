package com.clevertap.android.sdk.validation.pipeline

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.validation.ValidationOutcome
import org.json.JSONObject
/**
 * Sealed interface representing any validation result from a pipeline.
 * All validation results share a common outcome and drop logic.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal sealed interface ValidationResult {
    val outcome: ValidationOutcome
    
    fun shouldDrop(): Boolean = outcome is ValidationOutcome.Drop
}

/**
 * Sealed interface representing any normalization result.
 * Normalization results are internal to pipelines.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal sealed interface NormalizationResult

/**
 * Result of event name validation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class EventNameValidationResult(
    val cleanedName: String,
    override val outcome: ValidationOutcome
) : ValidationResult

/**
 * Result of event name normalization.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class EventNameNormalizationResult(
    val originalName: String?,
    val cleanedName: String,
    val modifications: Set<ModificationReason>
) : NormalizationResult

// ============================================================================
// Event Data Models
// ============================================================================
/**
 * Result of event data validation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class EventDataValidationResult(
    val cleanedData: JSONObject,
    override val outcome: ValidationOutcome,
) : ValidationResult

/**
 * Result of event data normalization.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class EventDataNormalizationResult(
    val cleanedData: JSONObject,
    val metrics: EventDataMetrics
) : NormalizationResult

/**
 * Metrics collected during event data normalization.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class EventDataMetrics(
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

// ============================================================================
// Property Key Models
// ============================================================================

/**
 * Result of property key validation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class PropertyKeyValidationResult(
    val cleanedKey: String,
    override val outcome: ValidationOutcome
) : ValidationResult

/**
 * Result of property key normalization.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class PropertyKeyNormalizationResult(
    val originalKey: String,
    val cleanedKey: String,
    val modifications: Set<KeyModification>,
    val wasRemoved: Boolean,
    val removalReason: RemovalReason?
) : NormalizationResult

// ============================================================================
// Shared Detail Models
// ============================================================================
/**
 * Details about a key that was modified during normalization.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class KeyModification(
    val originalKey: String,
    val cleanedKey: String,
    val reasons: List<ModificationReason>
)

/**
 * Details about a value that was modified during normalization.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class ValueModification(
    val key: String,
    val originalValue: String,
    val cleanedValue: String,
    val reasons: List<ModificationReason>
)

/**
 * Details about an item that was removed during normalization.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class RemovedItem(
    val key: String,
    val reason: RemovalReason,
    val originalValue: Any?
)

// ============================================================================
// Charged Event Items Models
// ============================================================================

/**
 * Result of charged event items validation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class ChargedEventItemsValidationResult(
    val itemsCount: Int,
    override val outcome: ValidationOutcome
) : ValidationResult

/**
 * Result of charged event items normalization.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class ChargedEventItemsNormalizationResult(
    val itemsCount: Int
) : NormalizationResult

// ============================================================================
// Enums
// ============================================================================

/**
 * Common modification reason enum used across all normalizers.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal enum class ModificationReason {
    INVALID_CHARACTERS_REMOVED,
    TRUNCATED_TO_MAX_LENGTH
}

/**
 * Reasons why data was removed during normalization.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal enum class RemovalReason {
    NULL_VALUE,
    EMPTY_VALUE,
    EMPTY_KEY,
    NON_PRIMITIVE_VALUE,
    INVALID_PHONE_NUMBER,
    INVALID_COUNTRY_CODE,
    RESTRICTED_KEY_NESTED_VALUE
}
