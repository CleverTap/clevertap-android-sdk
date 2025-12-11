package com.clevertap.android.sdk.validation.propertykey

import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.validation.DropReason
import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationError
import com.clevertap.android.sdk.validation.ValidationOutcome
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.ValidationResultFactory
import com.clevertap.android.sdk.validation.pipeline.PropertyKeyNormalizationResult

/**
 * Validator for multi-value property keys.
 * Extends base validation with multi-value restriction checks.
 */
class MultiValuePropertyKeyValidator(
    config: ValidationConfig
) : EventPropertyKeyValidator(config) {

    override fun validate(input: PropertyKeyNormalizationResult): ValidationOutcome {
        // Perform base validation first
        val baseOutcome = super.validate(input)

        // If base validation failed with drop, return it immediately
        if (baseOutcome is ValidationOutcome.Drop) {
            return baseOutcome
        }

        // Collect existing errors
        val errors = baseOutcome.errors.toMutableList()

        // Check multi-value restrictions
        val hasMultiValueViolation = checkMultiValueRestrictions(input, errors)

        return createOutcome(errors, hasMultiValueViolation)
    }

    /**
     * Checks if the key is restricted for multi-value operations.
     * Adds a drop error if the key is restricted.
     */
    private fun checkMultiValueRestrictions(
        input: PropertyKeyNormalizationResult,
        errors: MutableList<ValidationResult>
    ) : Boolean {
        val isRestricted = config.restrictedMultiValueFields?.any { restrictedField ->
            Utils.areNamesNormalizedEqual(input.cleanedKey, restrictedField)
        } ?: false

        if (!isRestricted) return false

        val error = ValidationResultFactory.create(
            ValidationError.RESTRICTED_MULTI_VALUE_KEY,
            input.cleanedKey
        )
        errors.add(error)
        return true
    }

    /**
     * Creates the validation outcome, treating multi-value restriction violations as drops.
     */
    private fun createOutcome(errors: List<ValidationResult>, hasMultiValueViolations : Boolean): ValidationOutcome {

        return when {
            hasMultiValueViolations -> ValidationOutcome.Drop(
                errors = errors,
                reason = DropReason.RESTRICTED_MULTI_VALUE_KEY
            )
            errors.isEmpty() -> ValidationOutcome.Success()
            else -> ValidationOutcome.Warning(errors)
        }
    }
}
