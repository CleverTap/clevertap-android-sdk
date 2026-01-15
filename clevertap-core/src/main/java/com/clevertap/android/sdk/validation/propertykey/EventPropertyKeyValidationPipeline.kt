package com.clevertap.android.sdk.validation.propertykey

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.validation.pipeline.PropertyKeyValidationResult
import com.clevertap.android.sdk.validation.pipeline.ValidationPipeline

/**
 * Pipeline for validating property keys.
 * Normalizes and validates property keys.
 *
 * Steps:
 * 1. Normalize property key (remove invalid chars, truncate)
 * 2. Validate normalized key
 * 3. Automatically report validation errors to the error reporter
 * 
 * @param errorReporter Error reporter for pushing errors to stack.
 *                      All validation errors are automatically pushed to this stack.
 * @param logger Logger for logging validation results
 */
class EventPropertyKeyValidationPipeline(
    private val errorReporter: ValidationResultStack,
    private val logger: ILogger
) : ValidationPipeline<String?, PropertyKeyValidationResult> {
    
    private val normalizer = EventPropertyKeyNormalizer()
    private val validator = EventPropertyKeyValidator()
    
    override fun execute(input: String?, config: ValidationConfig): PropertyKeyValidationResult {
        // Step 1: Normalize the input
        val normalizationResult = normalizer.normalize(input, config)
        
        // Step 2: Validate the normalized result
        val outcome = validator.validate(normalizationResult, config)
        
        // Step 3: Auto-report validation errors
        errorReporter.pushValidationResult(outcome.errors)
        
        // Step 4: Log validation results
        logValidationOutcome(
            logger = logger,
            tag = "PropertyKeyValidation",
            outcome = outcome,
        )

        return PropertyKeyValidationResult(
            cleanedKey = normalizationResult.cleanedKey,
            outcome = outcome
        )
    }
}
