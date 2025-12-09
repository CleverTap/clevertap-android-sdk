package com.clevertap.android.sdk.validation

import com.clevertap.android.sdk.Utils

/**
 * Validator checks the metrics against configured limits and reports errors.
 */
class Validator4(private val config: ValidationConfig) {
    
    /**
     * Checks if the provided metrics violate any configured limits.
     * Returns a validation outcome with all errors found.
     * Property validation never drops events, only warns about modifications.
     */
    fun checkLimits(metrics: PropertyValidationMetrics): ValidationOutcome {
        val errors = mutableListOf<ValidationResult>()
        
        // Check structural limits
        config.maxDepth?.let { limit ->
            if (metrics.maxDepth > limit) {
                val error = ValidationResultFactory2.create(
                    ValidationError.DEPTH_LIMIT_EXCEEDED,
                    metrics.maxDepth.toString(),
                    limit.toString()
                )
                errors.add(error)
            }
        }
        
        config.maxArrayKeyCount?.let { limit ->
            if (metrics.maxArrayKeyCount > limit) {
                val error = ValidationResultFactory2.create(
                    ValidationError.ARRAY_KEY_COUNT_LIMIT_EXCEEDED,
                    metrics.maxArrayKeyCount.toString(),
                    limit.toString()
                )
                errors.add(error)
            }
        }
        
        config.maxObjectKeyCount?.let { limit ->
            if (metrics.maxObjectKeyCount > limit) {
                val error = ValidationResultFactory2.create(
                    ValidationError.OBJECT_KEY_COUNT_LIMIT_EXCEEDED,
                    metrics.maxObjectKeyCount.toString(),
                    limit.toString()
                )
                errors.add(error)
            }
        }
        
        config.maxArrayLength?.let { limit ->
            if (metrics.maxArrayLength > limit) {
                val error = ValidationResultFactory2.create(
                    ValidationError.ARRAY_LENGTH_LIMIT_EXCEEDED,
                    metrics.maxArrayLength.toString(),
                    limit.toString()
                )
                errors.add(error)
            }
        }
        
        config.maxKVPairCount?.let { limit ->
            if (metrics.maxKVPairCount > limit) {
                val error = ValidationResultFactory2.create(
                    ValidationError.KV_PAIR_COUNT_LIMIT_EXCEEDED,
                    metrics.maxKVPairCount.toString(),
                    limit.toString()
                )
                errors.add(error)
            }
        }
        
        // Check data quality - keys modified
        metrics.keysModified.forEach { mod ->
            mod.reasons.forEach { reason ->
                when (reason) {
                    KeyModificationReason.INVALID_CHARACTERS_REMOVED -> {
                        val error = ValidationResultFactory2.create(
                            ValidationError.KEY_INVALID_CHARACTERS,
                            mod.originalKey
                        )
                        errors.add(error)
                    }
                    KeyModificationReason.TRUNCATED_TO_MAX_LENGTH -> {
                        config.maxKeyLength?.let { limit ->
                            val error = ValidationResultFactory2.create(
                                ValidationError.KEY_LENGTH_EXCEEDED,
                                mod.originalKey,
                                limit.toString()
                            )
                            errors.add(error)
                        }
                    }
                }
            }
        }
        
        // Check data quality - values modified
        metrics.valuesModified.forEach { mod ->
            mod.reasons.forEach { reason ->
                when (reason) {
                    // todo pass correct values to validation error
                    ValueModificationReason.INVALID_CHARACTERS_REMOVED -> {
                        val error = ValidationResultFactory2.create(
                            ValidationError.VALUE_INVALID_CHARACTERS,
                            mod.key
                        )
                        errors.add(error)
                    }
                    // todo pass correct values to validation error
                    ValueModificationReason.TRUNCATED_TO_MAX_LENGTH -> {
                        config.maxValueLength?.let { limit ->
                            val error = ValidationResultFactory2.create(
                                ValidationError.VALUE_CHARS_LIMIT_EXCEEDED,
                                mod.originalValue,
                                limit.toString()
                            )
                            errors.add(error)
                        }
                    }
                }
            }
        }
        
        // Check data quality - items removed
        metrics.itemsRemoved.forEach { item ->
            when (item.reason) {
                RemovalReason.NULL_VALUE -> {
                    val error = ValidationResultFactory2.create(
                        ValidationError.NULL_VALUE_REMOVED,
                        item.key
                    )
                    errors.add(error)
                }
                RemovalReason.EMPTY_VALUE -> {
                    val error = ValidationResultFactory2.create(
                        ValidationError.EMPTY_VALUE_REMOVED,
                        item.key
                    )
                    errors.add(error)
                }

                RemovalReason.EMPTY_KEY -> {
                    val error = ValidationResultFactory2.create(
                        ValidationError.EMPTY_KEY,
                        item.key
                    )
                    errors.add(error)
                }
                RemovalReason.NON_PRIMITIVE_VALUE -> {
                    val error = ValidationResultFactory2.create(
                        ValidationError.PROP_VALUE_NOT_PRIMITIVE,
                        item.key,
                        item.originalValue?.let { it::class.simpleName } ?: "null"
                    )
                    errors.add(error)

                }

                RemovalReason.INVALID_PHONE_NUMBER -> {
                    val error = ValidationResultFactory2.create(
                        ValidationError.INVALID_PHONE,
                    )
                    errors.add(error)

                }

                // todo should not be removal reason
                RemovalReason.INVALID_COUNTRY_CODE -> {
                    val error = ValidationResultFactory2.create(
                        ValidationError.INVALID_COUNTRY_CODE,
                    )
                    errors.add(error)
                }

            }
        }

        // Property validation doesn't drop events, only warns about modifications/limits
        return if (errors.isEmpty()) {
            ValidationOutcome.Success()
        } else {
            ValidationOutcome.Warning(errors = errors)
        }
    }
    
    /**
     * Validates event name and generates validation errors if any rules are violated.
     * This should be called BEFORE normalization to check for null/restricted/discarded names.
     *
     * @param eventName The event name to validate
     * @return ValidationOutcome indicating whether to proceed, warn, or drop the event
     */
    fun validateEventName(eventName: String?): ValidationOutcome {
        val errors = mutableListOf<ValidationResult>()
        
        // Check if event name is null
        if (eventName == null) {
            val error = ValidationResultFactory2.create(ValidationError.EVENT_NAME_NULL)
            errors.add(error)
            return ValidationOutcome.Drop(
                errors = errors,
                reason = DropReason.NULL_EVENT_NAME
            )
        }
        
        // Check if event name is restricted
        val isRestricted = config.restrictedEventNames?.any { restrictedName ->
            Utils.areNamesNormalizedEqual(eventName, restrictedName)
        } ?: false
        
        if (isRestricted) {
            val error = ValidationResultFactory2.create(
                ValidationError.RESTRICTED_EVENT_NAME,
                eventName
            )
            errors.add(error)
            return ValidationOutcome.Drop(
                errors = errors,
                reason = DropReason.RESTRICTED_EVENT_NAME
            )
        }
        
        // Check if event name is discarded
        val isDiscarded = config.discardedEventNames?.any { discardedName ->
            Utils.areNamesNormalizedEqual(eventName, discardedName)
        } ?: false
        
        if (isDiscarded) {
            val error = ValidationResultFactory2.create(
                ValidationError.DISCARDED_EVENT_NAME,
                eventName
            )
            errors.add(error)
            return ValidationOutcome.Drop(
                errors = errors,
                reason = DropReason.DISCARDED_EVENT_NAME
            )
        }
        
        return ValidationOutcome.Success()
    }

    /**
     * Validates a property key normalization result and generates validation errors for any modifications or removals.
     * This should be called AFTER normalization to report what happened during cleaning.
     *
     * @param result The key normalization result to validate
     * @return ValidationOutcome indicating whether to proceed, warn, or drop the property
     */
    fun validatePropertyKey(result: KeyNormalizationResult): ValidationOutcome {
        val errors = mutableListOf<ValidationResult>()

        // Report key modifications
        // todo maybe also pass the list of invalid characters
        result.modifications.forEach { modification ->
            modification.reasons.forEach { reason ->
                val error = when (reason) {
                    KeyModificationReason.INVALID_CHARACTERS_REMOVED -> {
                        ValidationResultFactory2.create(
                            ValidationError.KEY_INVALID_CHARACTERS,
                            modification.originalKey,
                            modification.cleanedKey
                        )
                    }
                    KeyModificationReason.TRUNCATED_TO_MAX_LENGTH -> {
                        ValidationResultFactory2.create(
                            ValidationError.KEY_LENGTH_EXCEEDED,
                            modification.originalKey,
                            modification.cleanedKey
                        )
                    }
                }
                errors.add(error)
            }
        }

        result.removals.forEach { removals ->
            val error = ValidationResultFactory2.create(ValidationError.EMPTY_KEY_ABORT)
            errors.add(error)
            return ValidationOutcome.Drop(
                errors = errors,
                reason = DropReason.EMPTY_KEY
            )
        }

        // If there were modifications but key survived, return warnings
        return if (errors.isNotEmpty()) {
            ValidationOutcome.Warning(errors = errors)
        } else {
            ValidationOutcome.Success()
        }
    }

    fun validateMultiValuePropertyKey(result: KeyNormalizationResult): ValidationOutcome {
        val propertyValidation = validatePropertyKey(result)
        if (propertyValidation is ValidationOutcome.Drop) {
            return propertyValidation
        }

        val errors = mutableListOf<ValidationResult>()
        errors.addAll(propertyValidation.errors)

        val cleanedKey = result.cleanedKey

        val isRestrictedMultiValue = config.restrictedMultiValueFields?.any { restrictedField ->
            Utils.areNamesNormalizedEqual(cleanedKey, restrictedField)
        } ?: false

        if (isRestrictedMultiValue) {
            val error = ValidationResultFactory2.create(
                ValidationError.RESTRICTED_MULTI_VALUE_KEY,
                cleanedKey
            )
            errors.add(error)
            return ValidationOutcome.Drop(
                errors = errors,
                reason = DropReason.RESTRICTED_MULTI_VALUE_KEY
            )
        }

        // Return warnings if any, otherwise success
        return if (errors.isNotEmpty()) {
            ValidationOutcome.Warning(errors = errors)
        } else {
            ValidationOutcome.Success()
        }
    }
    
    /**
     * Validates event name metrics after normalization.
     * This should be called AFTER normalization to check for truncation and other modifications.
     *
     * @param metrics The metrics from event name normalization
     * @return ValidationOutcome with warnings if name was modified
     */
    fun validateEventNameMetrics(metrics: EventNameMetrics): ValidationOutcome {
        val errors = mutableListOf<ValidationResult>()
        
        // Check if name was truncated due to length
        // todo - pass correct values
        if (metrics.modifications.contains(EventNameModificationReason.TRUNCATED_TO_MAX_LENGTH)) {
            val error = ValidationResultFactory2.create(
                ValidationError.EVENT_NAME_TOO_LONG,
                metrics.originalLength.toString(),
                metrics.maxLength?.toString() ?: "unknown"
            )
            errors.add(error)
        }
        
        return if (errors.isEmpty()) {
            ValidationOutcome.Success()
        } else {
            ValidationOutcome.Warning(errors = errors)
        }
    }
}