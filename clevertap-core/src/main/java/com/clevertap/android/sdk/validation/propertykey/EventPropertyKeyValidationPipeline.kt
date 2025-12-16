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
 * @param config Validation configuration
 * @param errorReporter Error reporter for pushing errors to stack.
 *                      All validation errors are automatically pushed to this stack.
 * @param logger Logger for logging validation results
 */
class EventPropertyKeyValidationPipeline(
    config: ValidationConfig,
    private val errorReporter: ValidationResultStack,
    private val logger: ILogger
) : ValidationPipeline<String?, PropertyKeyValidationResult> {
    
    private val normalizer = EventPropertyKeyNormalizer(config)
    private val validator = EventPropertyKeyValidator(config)
    
    override fun execute(input: String?): PropertyKeyValidationResult {
        // Step 1: Normalize the input
        val normalizationResult = normalizer.normalize(input)
        
        // Step 2: Validate the normalized result
        val outcome = validator.validate(normalizationResult)
        
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
