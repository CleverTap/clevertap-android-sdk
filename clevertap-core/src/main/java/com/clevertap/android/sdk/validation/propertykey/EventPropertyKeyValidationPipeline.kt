package com.clevertap.android.sdk.validation.propertykey

import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.pipeline.PropertyKeyValidationResult
import com.clevertap.android.sdk.validation.pipeline.ValidationPipeline

/**
 * Base pipeline for validating property keys.
 * Normalizes and validates property keys.
 *
 * Steps:
 * 1. Normalize property key (remove invalid chars, truncate)
 * 2. Validate normalized key
 */
open class EventPropertyKeyValidationPipeline(
    config: ValidationConfig
) : ValidationPipeline<String?, PropertyKeyValidationResult> {
    
    protected val normalizer = EventPropertyKeyNormalizer(config)
    protected open val validator = EventPropertyKeyValidator(config)
    
    override fun execute(input: String?): PropertyKeyValidationResult {
        // Normalize
        val normalizationResult = normalizer.normalize(input)
        
        // Validate
        val outcome = validator.validate(normalizationResult)
        
        return PropertyKeyValidationResult(
            cleanedKey = normalizationResult.cleanedKey,
            outcome = outcome
        )
    }
}
