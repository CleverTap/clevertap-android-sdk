package com.clevertap.android.sdk.validation.propertykey

import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.validation.DropReason
import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationError
import com.clevertap.android.sdk.validation.ValidationOutcome
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.ValidationResultFactory
import com.clevertap.android.sdk.validation.pipeline.ModificationReason
import com.clevertap.android.sdk.validation.pipeline.PropertyKeyNormalizationResult
import com.clevertap.android.sdk.validation.pipeline.Validator

/**
 * Validates event property keys after normalization.
 * Checks for empty keys, modifications, and multi-value restrictions.
 */
class EventPropertyKeyValidator(
    private val config: ValidationConfig
) : Validator<PropertyKeyNormalizationResult> {

    override fun validate(input: PropertyKeyNormalizationResult): ValidationOutcome {
        val errors = mutableListOf<ValidationResult>()

        // Check if key was removed
        if (input.wasRemoved) {
            val error = ValidationResultFactory.create(ValidationError.EMPTY_KEY_ABORT)
            errors.add(error)
            return ValidationOutcome.Drop(
                errors = errors,
                reason = DropReason.EMPTY_KEY
            )
        }

        // Report key modifications
        input.modifications.forEach { modification ->
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
                        config.maxKeyLength?.let { limit ->
                            ValidationResultFactory.create(
                                ValidationError.KEY_LENGTH_EXCEEDED,
                                modification.originalKey,
                                limit.toString()
                            )
                        } ?: return@forEach
                    }
                }
                errors.add(error)
            }
        }

        return if (errors.isEmpty()) {
            ValidationOutcome.Success()
        } else {
            ValidationOutcome.Warning(errors)
        }
    }

    /**
     * Validates a multi-value property key.
     * Checks normal validation plus multi-value restrictions.
     *
     * @param input The key normalization result
     * @return ValidationOutcome with Drop if key is restricted for multi-value
     */
    fun validateMultiValueKey(input: PropertyKeyNormalizationResult): ValidationOutcome {
        val basicValidation = validate(input)

        // If basic validation failed with drop, return it
        if (basicValidation is ValidationOutcome.Drop) {
            return basicValidation
        }

        val errors = basicValidation.errors.toMutableList()

        // Check if key is restricted for multi-value
        val isRestricted = config.restrictedMultiValueFields?.any { restrictedField ->
            Utils.areNamesNormalizedEqual(input.cleanedKey, restrictedField)
        } ?: false

        if (isRestricted) {
            val error = ValidationResultFactory.create(
                ValidationError.RESTRICTED_MULTI_VALUE_KEY,
                input.cleanedKey
            )
            errors.add(error)
            return ValidationOutcome.Drop(
                errors = errors,
                reason = DropReason.RESTRICTED_MULTI_VALUE_KEY
            )
        }

        return if (errors.isEmpty()) {
            ValidationOutcome.Success()
        } else {
            ValidationOutcome.Warning(errors)
        }
    }
}