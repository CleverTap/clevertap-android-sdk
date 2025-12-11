package com.clevertap.android.sdk.validation.eventname

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
 * 3. Auto-report validation errors (if error reporter provided)
 * 
 * @param config Validation configuration
 * @param errorReporter Optional error reporter for automatic error logging.
 *                      Pass null to disable automatic error reporting.
 */
class EventNameValidationPipeline(
    config: ValidationConfig,
    private val errorReporter: ValidationResultStack
) : ValidationPipeline<String?, EventNameValidationResult> {
    
    private val normalizer = EventNameNormalizer(config)
    private val validator = EventNameValidator(config)
    
    override fun execute(input: String?): EventNameValidationResult {
        val normalizationResult = normalizer.normalize(input)
        val validationOutcome = validator.validate(normalizationResult)
        
        // Auto-report validation errors if reporter is provided
        errorReporter.pushValidationResult(validationOutcome.errors)
        
        return EventNameValidationResult(
            cleanedName = normalizationResult.cleanedName,
            outcome = validationOutcome
        )
    }
}
