package com.clevertap.android.sdk.validation.propertykey

import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.validation.pipeline.PropertyKeyValidationResult
import com.clevertap.android.sdk.validation.pipeline.ValidationPipeline

/**
 * Base pipeline for validating property keys.
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
 */
open class EventPropertyKeyValidationPipeline(
    config: ValidationConfig,
    private val errorReporter: ValidationResultStack
) : ValidationPipeline<String?, PropertyKeyValidationResult> {
    
    protected val normalizer = EventPropertyKeyNormalizer(config)
    protected open val validator = EventPropertyKeyValidator(config)
    
    override fun execute(input: String?): PropertyKeyValidationResult {
        // Normalize
        val normalizationResult = normalizer.normalize(input)
        
        // Validate
        val outcome = validator.validate(normalizationResult)
        
        // Auto-report validation errors
        errorReporter.pushValidationResult(outcome.errors)
        
        return PropertyKeyValidationResult(
            cleanedKey = normalizationResult.cleanedKey,
            outcome = outcome
        )
    }
}
