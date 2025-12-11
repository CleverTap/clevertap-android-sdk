package com.clevertap.android.sdk.validation.eventdata

import com.clevertap.android.sdk.validation.ValidationConfig
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
 */
class EventDataValidationPipeline(
    config: ValidationConfig
) : ValidationPipeline<Map<*, *>?, EventDataValidationResult> {
    
    private val normalizer = EventDataNormalizer(config)
    private val validator = EventDataValidator(config)
    
    override fun execute(input: Map<*, *>?): EventDataValidationResult {
        // Normalize
        val normalizationResult = normalizer.normalize(input)
        
        // Validate
        val outcome = validator.validate(normalizationResult)
        
        return EventDataValidationResult(
            cleanedData = normalizationResult.cleanedData,
            outcome = outcome,
        )
    }
}
