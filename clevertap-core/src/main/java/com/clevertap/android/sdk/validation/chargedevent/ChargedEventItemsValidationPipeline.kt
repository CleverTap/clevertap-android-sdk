package com.clevertap.android.sdk.validation.chargedevent

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.validation.pipeline.ChargedEventItemsValidationResult
import com.clevertap.android.sdk.validation.pipeline.ValidationPipeline

/**
 * Pipeline for validating charged event items.
 * Validates the size of items array in a charged event.
 *
 * Steps:
 * 1. Normalize items (count items)
 * 2. Validate items count against maximum allowed limit (50)
 * 3. Automatically report validation errors to the error reporter
 *
 * @param config Validation configuration
 * @param errorReporter Error reporter for pushing errors to stack.
 *                      All validation errors are automatically pushed to this stack.
 * @param logger Logger for logging validation results
 */
open class ChargedEventItemsValidationPipeline(
    config: ValidationConfig,
    private val errorReporter: ValidationResultStack,
    private val logger: ILogger
) : ValidationPipeline<List<*>?, ChargedEventItemsValidationResult> {

    private val normalizer = ChargedEventItemsNormalizer(config)
    private val validator = ChargedEventItemsValidator(config)

    override fun execute(input: List<*>?): ChargedEventItemsValidationResult {
        // Step 1: Normalize the input
        val normalizationResult = normalizer.normalize(input)

        // Step 2: Validate the normalized result
        val outcome = validator.validate(normalizationResult)

        // Step 3: Auto-report validation errors
        errorReporter.pushValidationResult(outcome.errors)

        // Step 4: Log validation results
        logValidationOutcome(
            logger = logger,
            tag = "ChargedEventItemsValidation",
            outcome = outcome
        )

        return ChargedEventItemsValidationResult(
            itemsCount = normalizationResult.itemsCount,
            outcome = outcome
        )
    }
}
