package com.clevertap.android.sdk.validation.propertykey

import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationOutcome

/**
 * Pipeline for validating property keys.
 * Normalizes and validates property keys, with support for multi-value restrictions.
 *
 * Steps:
 * 1. Normalize property key (remove invalid chars, truncate)
 * 2. Validate normalized key
 * 3. Optionally check multi-value restrictions
 */
class EventPropertyKeyValidationPipeline(
    config: ValidationConfig
) : com.clevertap.android.sdk.validation.pipeline.ValidationPipeline<PropertyKeyValidationInput, PropertyKeyValidationResult> {
    
    private val normalizer = EventPropertyKeyNormalizer(config)
    private val validator =
        EventPropertyKeyValidator(
            config
        )
    
    override fun execute(input: PropertyKeyValidationInput): PropertyKeyValidationResult {
        // Normalize
        val normalizationResult = normalizer.normalize(input.key)
        
        // Validate (with multi-value check if requested)
        val outcome = if (input.isMultiValue) {
            validator.validateMultiValueKey(normalizationResult)
        } else {
            validator.validate(normalizationResult)
        }
        
        return PropertyKeyValidationResult(
            originalKey = input.key,
            cleanedKey = normalizationResult.cleanedKey,
            outcome = outcome,
        )
    }
}

/**
 * Input for property key validation.
 *
 * @param key The property key to validate
 * @param isMultiValue Whether to check multi-value restrictions
 */
data class PropertyKeyValidationInput(
    val key: String?,
    val isMultiValue: Boolean = false
)

/**
 * Result of property key validation.
 */
data class PropertyKeyValidationResult(
    val originalKey: String?,
    val cleanedKey: String,
    val outcome: ValidationOutcome,
) {
    fun shouldDrop(): Boolean = outcome is ValidationOutcome.Drop
}
