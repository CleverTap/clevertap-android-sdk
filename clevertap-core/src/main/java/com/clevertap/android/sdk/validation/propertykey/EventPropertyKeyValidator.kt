package com.clevertap.android.sdk.validation.propertykey

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
class EventPropertyKeyValidator : Validator<PropertyKeyNormalizationResult> {

    override fun validate(input: PropertyKeyNormalizationResult, config: ValidationConfig): ValidationOutcome {
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
                                limit.toString(),
                                modification.cleanedKey
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
}