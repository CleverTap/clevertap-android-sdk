package com.clevertap.android.sdk.validation.eventname

import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationOutcome

/**
 * Pipeline for validating event names.
 * Normalizes and validates event names, checking for restrictions and discard lists.
 *
 * Steps:
 * 1. Normalize event name (remove invalid chars, truncate)
 * 2. Validate normalized name (includes restriction checks)
 */
class EventNameValidationPipeline(
    config: ValidationConfig
) : com.clevertap.android.sdk.validation.pipeline.ValidationPipeline<String?, EventNameValidationResult> {
    
    private val normalizer = EventNameNormalizer(config)
    private val validator =
        EventNameValidator(config)
    
    override fun execute(input: String?): EventNameValidationResult {
        val normalizationResult = normalizer.normalize(input)
        val validationOutcome = validator.validate(normalizationResult)
        
        return EventNameValidationResult(
            originalName = input,
            cleanedName = normalizationResult.cleanedName,
            outcome = validationOutcome
        )
    }
}

/**
 * Result of event name validation.
 */
data class EventNameValidationResult(
    val originalName: String?,
    val cleanedName: String?,
    val outcome: ValidationOutcome,
) {
    fun shouldDrop(): Boolean = outcome is ValidationOutcome.Drop
}

