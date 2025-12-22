package com.clevertap.android.sdk.validation.eventdata

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.validation.DropReason
import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationOutcome
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.validation.pipeline.EventDataValidationResult

/**
 * Pipeline for validating multi-value property keys.
 * Extends base validation with multi-value restriction checks and empty data handling.
 *
 * Steps:
 * 1. Normalize property key (remove invalid chars, truncate)
 * 2. Validate normalized key
 * 3. Check multi-value restrictions
 * 4. Drop if cleaned data is empty
 * 5. Automatically report validation errors to the error reporter
 *
 * @param errorReporter Error reporter for pushing errors to stack.
 *                      All validation errors are automatically pushed to this stack.
 * @param logger Logger for logging validation results
 */
class MultiValueDataValidationPipeline(
    errorReporter: ValidationResultStack,
    logger: ILogger
) : EventDataValidationPipeline(errorReporter, logger) {

    override fun execute(input: Map<*, *>?, config: ValidationConfig): EventDataValidationResult {
        val result = super.execute(input, config)

        // Drop event if all properties were removed during validation
        if (result.cleanedData.length() == 0) {
            return result.copy(
                outcome = ValidationOutcome.Drop(
                    errors = result.outcome.errors,
                    reason = DropReason.EMPTY_EVENT_DATA
                )
            )
        }

        return result
    }
}