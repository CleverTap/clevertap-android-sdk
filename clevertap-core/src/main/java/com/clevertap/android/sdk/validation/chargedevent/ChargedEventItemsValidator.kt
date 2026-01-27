package com.clevertap.android.sdk.validation.chargedevent

import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationError
import com.clevertap.android.sdk.validation.ValidationOutcome
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.ValidationResultFactory
import com.clevertap.android.sdk.validation.pipeline.ChargedEventItemsNormalizationResult
import com.clevertap.android.sdk.validation.pipeline.Validator

/**
 * Validates charged event items after normalization.
 * Checks if the items count exceeds the maximum allowed limit.
 * Note: Items validation never drops events, only warns.
 */
class ChargedEventItemsValidator : Validator<ChargedEventItemsNormalizationResult> {

    override fun validate(input: ChargedEventItemsNormalizationResult, config: ValidationConfig): ValidationOutcome {
        val errors = mutableListOf<ValidationResult>()

        // Check items count limit
        config.maxChargedEventItemsCount?.let { maxCount ->
            if (input.itemsCount > maxCount) {
                val error = ValidationResultFactory.create(ValidationError.CHARGED_EVENT_TOO_MANY_ITEMS)
                errors.add(error)
            }
        }

        // Items validation never drops events, only warns
        return if (errors.isEmpty()) {
            ValidationOutcome.Success()
        } else {
            ValidationOutcome.Warning(errors)
        }
    }
}
