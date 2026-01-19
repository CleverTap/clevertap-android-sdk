package com.clevertap.android.sdk.validation.eventname

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.validation.pipeline.EventNameValidationResult
import com.clevertap.android.sdk.validation.pipeline.ValidationPipeline

/**
 * Pipeline for validating event names.
 * Normalizes and validates event names, checking for restrictions and discard lists.
 *
 * Steps:
 * 1. Normalize event name (remove invalid chars, truncate)
 * 2. Validate normalized name (includes restriction checks)
 * 3. Automatically report validation errors to the error reporter
 * 
 * @param errorReporter Error reporter for pushing errors to stack.
 *                      All validation errors are automatically pushed to this stack.
 * @param logger Logger for logging validation results
 * 
 * @return EventNameValidationResult containing:
 *   - cleanedName: The normalized/cleaned event name (empty string if input was null)
 *   - outcome: ValidationOutcome indicating Success/Warning/Drop with errors
 * 
 * The pipeline always returns cleaned data even if validation fails.
 * Caller should check outcome.shouldDrop() to determine if the event should be dropped.
 */
class EventNameValidationPipeline(
    private val errorReporter: ValidationResultStack,
    private val logger: ILogger
) : ValidationPipeline<String?, EventNameValidationResult> {
    
    private val normalizer = EventNameNormalizer()
    private val validator = EventNameValidator()
    
    override fun execute(input: String?, config: ValidationConfig): EventNameValidationResult {
        // Step 1: Normalize the input
        val normalizationResult = normalizer.normalize(input, config)
        
        // Step 2: Validate the normalized result
        val validationOutcome = validator.validate(normalizationResult, config)
        
        // Step 3: Auto-report validation errors
        errorReporter.pushValidationResult(validationOutcome.errors)
        
        // Step 4: Log validation results
        logValidationOutcome(
            logger = logger,
            tag = "EventNameValidation",
            outcome = validationOutcome,
        )

        // Return cleaned data + outcome (caller checks outcome.shouldDrop())
        return EventNameValidationResult(
            cleanedName = normalizationResult.cleanedName,
            outcome = validationOutcome
        )
    }
}
