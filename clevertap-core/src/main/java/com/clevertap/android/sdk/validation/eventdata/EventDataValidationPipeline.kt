package com.clevertap.android.sdk.validation.eventdata

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.validation.pipeline.EventDataValidationResult
import com.clevertap.android.sdk.validation.pipeline.ValidationPipeline

/**
 * Pipeline for validating event data (properties).
 * Normalizes and validates the entire event data structure including nested objects and arrays.
 *
 * Steps:
 * 1. Normalize event data (clean keys/values, remove nulls/empties)
 * 2. Validate structural limits (depth, array size, etc.)
 * 3. Report modifications and removals
 * 4. Automatically report validation errors to the error reporter
 * 
 * @param config Validation configuration
 * @param errorReporter Error reporter for pushing errors to stack.
 *                      All validation errors are automatically pushed to this stack.
 * @param logger Logger for logging validation results
 */
open class EventDataValidationPipeline(
    config: ValidationConfig,
    private val errorReporter: ValidationResultStack,
    private val logger: ILogger
) : ValidationPipeline<Map<*, *>?, EventDataValidationResult> {
    
    private val normalizer = EventDataNormalizer(config)
    private val validator = EventDataValidator(config)
    
    override fun execute(input: Map<*, *>?): EventDataValidationResult {
        // Step 1: Normalize the input
        val normalizationResult = normalizer.normalize(input)
        
        // Step 2: Validate the normalized result
        val outcome = validator.validate(normalizationResult)
        
        // Step 3: Auto-report validation errors
        errorReporter.pushValidationResult(outcome.errors)
        
        // Step 4: Log validation results
        logValidationOutcome(
            logger = logger,
            tag = "EventDataValidation",
            outcome = outcome
        )

        return EventDataValidationResult(
            cleanedData = normalizationResult.cleanedData,
            outcome = outcome,
        )
    }
}
