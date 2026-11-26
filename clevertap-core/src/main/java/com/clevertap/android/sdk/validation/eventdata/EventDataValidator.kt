package com.clevertap.android.sdk.validation.eventdata

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationError
import com.clevertap.android.sdk.validation.ValidationOutcome
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.ValidationResultFactory
import com.clevertap.android.sdk.validation.pipeline.EventDataMetrics
import com.clevertap.android.sdk.validation.pipeline.EventDataNormalizationResult
import com.clevertap.android.sdk.validation.pipeline.KeyModification
import com.clevertap.android.sdk.validation.pipeline.ModificationReason
import com.clevertap.android.sdk.validation.pipeline.RemovalReason
import com.clevertap.android.sdk.validation.pipeline.Validator
import com.clevertap.android.sdk.validation.pipeline.ValueModification

/**
 * Validates event data after normalization.
 * Checks structural limits and reports all modifications/removals.
 * Note: Event data validation never drops events, only warns.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class EventDataValidator : Validator<EventDataNormalizationResult> {

    override fun validate(input: EventDataNormalizationResult, config: ValidationConfig): ValidationOutcome {
        val errors = mutableListOf<ValidationResult>()

        // Check structural limits
        checkStructuralLimits(
            metrics = input.metrics,
            maxDepth = config.maxDepth,
            maxArrayKeyCount = config.maxArrayKeyCount,
            maxObjectKeyCount = config.maxObjectKeyCount,
            maxArrayLength = config.maxArrayLength,
            maxKVPairCount = config.maxKVPairCount,
            errors = errors
        )

        // Check data quality - keys modified
        checkKeyModifications(
            metrics = input.metrics,
            maxKeyLength = config.maxKeyLength,
            errors = errors
        )

        // Check data quality - values modified
        checkValueModifications(
            metrics = input.metrics,
            maxValueLength = config.maxValueLength,
            errors = errors
        )

        // Check data quality - items removed
        checkItemRemovals(input.metrics, errors)

        // Event data validation never drops events, only warns
        return if (errors.isEmpty()) {
            ValidationOutcome.Success()
        } else {
            ValidationOutcome.Warning(errors)
        }
    }

    private fun checkStructuralLimits(
        metrics: EventDataMetrics,
        maxDepth: Int?,
        maxArrayKeyCount: Int?,
        maxObjectKeyCount: Int?,
        maxArrayLength: Int?,
        maxKVPairCount: Int?,
        errors: MutableList<ValidationResult>
    ) {
        checkLimit(
            actual = metrics.maxDepth,
            limit = maxDepth,
            error = ValidationError.DEPTH_LIMIT_EXCEEDED,
            errors = errors
        )

        checkLimit(
            actual = metrics.maxArrayKeyCount,
            limit = maxArrayKeyCount,
            error = ValidationError.ARRAY_KEY_COUNT_LIMIT_EXCEEDED,
            errors = errors
        )

        checkLimit(
            actual = metrics.maxObjectKeyCount,
            limit = maxObjectKeyCount,
            error = ValidationError.OBJECT_KEY_COUNT_LIMIT_EXCEEDED,
            errors = errors
        )

        checkLimit(
            actual = metrics.maxArrayLength,
            limit = maxArrayLength,
            error = ValidationError.ARRAY_LENGTH_LIMIT_EXCEEDED,
            errors = errors
        )

        checkLimit(
            actual = metrics.maxKVPairCount,
            limit = maxKVPairCount,
            error = ValidationError.KV_PAIR_COUNT_LIMIT_EXCEEDED,
            errors = errors
        )
    }

    /** Adds [error] to [errors] when a configured [limit] is exceeded by [actual]. */
    private fun checkLimit(
        actual: Int,
        limit: Int?,
        error: ValidationError,
        errors: MutableList<ValidationResult>
    ) {
        limit?.let {
            if (actual > it) {
                errors.add(ValidationResultFactory.create(error, actual.toString(), it.toString()))
            }
        }
    }

    private fun checkKeyModifications(
        metrics: EventDataMetrics,
        maxKeyLength: Int?,
        errors: MutableList<ValidationResult>
    ) {
        metrics.keysModified.forEach { modification ->
            modification.reasons.forEach { reason ->
                val error = keyModificationError(
                    modification = modification,
                    reason = reason,
                    maxKeyLength = maxKeyLength
                )
                if (error != null) {
                    errors.add(error)
                }
            }
        }
    }

    /** Returns null for a truncation when no max key length is configured, which reports nothing. */
    private fun keyModificationError(
        modification: KeyModification,
        reason: ModificationReason,
        maxKeyLength: Int?
    ): ValidationResult? {
        return when (reason) {
            ModificationReason.INVALID_CHARACTERS_REMOVED -> {
                ValidationResultFactory.create(
                    ValidationError.KEY_INVALID_CHARACTERS,
                    modification.originalKey,
                    modification.cleanedKey
                )
            }
            ModificationReason.TRUNCATED_TO_MAX_LENGTH -> {
                maxKeyLength?.let { limit ->
                    ValidationResultFactory.create(
                        ValidationError.KEY_LENGTH_EXCEEDED,
                        modification.originalKey,
                        limit.toString(),
                        modification.cleanedKey
                    )
                }
            }
        }
    }

    private fun checkValueModifications(
        metrics: EventDataMetrics,
        maxValueLength: Int?,
        errors: MutableList<ValidationResult>
    ) {
        metrics.valuesModified.forEach { modification ->
            modification.reasons.forEach { reason ->
                val error = valueModificationError(
                    modification = modification,
                    reason = reason,
                    maxValueLength = maxValueLength
                )
                if (error != null) {
                    errors.add(error)
                }
            }
        }
    }

    /** Returns null for a truncation when no max value length is configured, which reports nothing. */
    private fun valueModificationError(
        modification: ValueModification,
        reason: ModificationReason,
        maxValueLength: Int?
    ): ValidationResult? {
        return when (reason) {
            ModificationReason.INVALID_CHARACTERS_REMOVED -> {
                ValidationResultFactory.create(
                    ValidationError.VALUE_INVALID_CHARACTERS,
                    modification.originalValue,
                    modification.key,
                    modification.cleanedValue
                )
            }
            ModificationReason.TRUNCATED_TO_MAX_LENGTH -> {
                maxValueLength?.let { limit ->
                    ValidationResultFactory.create(
                        ValidationError.VALUE_CHARS_LIMIT_EXCEEDED,
                        modification.originalValue,
                        modification.key,
                        limit.toString(),
                        modification.cleanedValue
                    )
                }
            }
        }
    }

    private fun checkItemRemovals(metrics: EventDataMetrics, errors: MutableList<ValidationResult>) {
        metrics.itemsRemoved.forEach { item ->
            val error = when (item.reason) {
                RemovalReason.NULL_VALUE -> {
                    ValidationResultFactory.create(
                        ValidationError.NULL_VALUE_REMOVED,
                        item.key
                    )
                }
                RemovalReason.EMPTY_VALUE -> {
                    ValidationResultFactory.create(
                        ValidationError.EMPTY_VALUE_REMOVED,
                        item.key
                    )
                }
                RemovalReason.EMPTY_KEY -> {
                    ValidationResultFactory.create(
                        ValidationError.EMPTY_KEY,
                        item.key
                    )
                }
                RemovalReason.NON_PRIMITIVE_VALUE -> {
                    ValidationResultFactory.create(
                        ValidationError.PROP_VALUE_NOT_PRIMITIVE,
                        item.key,
                        typeNameOf(item.originalValue)
                    )
                }
                RemovalReason.INVALID_PHONE_NUMBER -> {
                    ValidationResultFactory.create(ValidationError.INVALID_PHONE)
                }
                RemovalReason.INVALID_COUNTRY_CODE -> {
                    ValidationResultFactory.create(ValidationError.INVALID_COUNTRY_CODE, item.originalValue.toString())
                }

                RemovalReason.RESTRICTED_KEY_NESTED_VALUE -> {
                    ValidationResultFactory.create(ValidationError.RESTRICTED_MULTI_VALUE_KEY, item.key)
                }
            }
            errors.add(error)
        }
    }

    private fun typeNameOf(value: Any?): String = value?.let { it::class.simpleName } ?: "null"
}
