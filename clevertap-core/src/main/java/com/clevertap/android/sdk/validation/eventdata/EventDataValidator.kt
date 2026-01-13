package com.clevertap.android.sdk.validation.eventdata

import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationError
import com.clevertap.android.sdk.validation.ValidationOutcome
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.ValidationResultFactory
import com.clevertap.android.sdk.validation.pipeline.EventDataMetrics
import com.clevertap.android.sdk.validation.pipeline.EventDataNormalizationResult
import com.clevertap.android.sdk.validation.pipeline.ModificationReason
import com.clevertap.android.sdk.validation.pipeline.RemovalReason
import com.clevertap.android.sdk.validation.pipeline.Validator

/**
 * Validates event data after normalization.
 * Checks structural limits and reports all modifications/removals.
 * Note: Event data validation never drops events, only warns.
 */
class EventDataValidator : Validator<EventDataNormalizationResult> {

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
        maxDepth?.let { limit ->
            if (metrics.maxDepth > limit) {
                val error = ValidationResultFactory.create(
                    ValidationError.DEPTH_LIMIT_EXCEEDED,
                    metrics.maxDepth.toString(),
                    limit.toString()
                )
                errors.add(error)
            }
        }

        maxArrayKeyCount?.let { limit ->
            if (metrics.maxArrayKeyCount > limit) {
                val error = ValidationResultFactory.create(
                    ValidationError.ARRAY_KEY_COUNT_LIMIT_EXCEEDED,
                    metrics.maxArrayKeyCount.toString(),
                    limit.toString()
                )
                errors.add(error)
            }
        }

        maxObjectKeyCount?.let { limit ->
            if (metrics.maxObjectKeyCount > limit) {
                val error = ValidationResultFactory.create(
                    ValidationError.OBJECT_KEY_COUNT_LIMIT_EXCEEDED,
                    metrics.maxObjectKeyCount.toString(),
                    limit.toString()
                )
                errors.add(error)
            }
        }

        maxArrayLength?.let { limit ->
            if (metrics.maxArrayLength > limit) {
                val error = ValidationResultFactory.create(
                    ValidationError.ARRAY_LENGTH_LIMIT_EXCEEDED,
                    metrics.maxArrayLength.toString(),
                    limit.toString()
                )
                errors.add(error)
            }
        }

        maxKVPairCount?.let { limit ->
            if (metrics.maxKVPairCount > limit) {
                val error = ValidationResultFactory.create(
                    ValidationError.KV_PAIR_COUNT_LIMIT_EXCEEDED,
                    metrics.maxKVPairCount.toString(),
                    limit.toString()
                )
                errors.add(error)
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
                val error = when (reason) {
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
                        } ?: return@forEach
                    }
                }
                errors.add(error)
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
                val error = when (reason) {
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
                        } ?: return@forEach
                    }
                }
                errors.add(error)
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
                        item.originalValue?.let { it::class.simpleName } ?: "null"
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
}