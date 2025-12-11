package com.clevertap.android.sdk.validation.propertykey

import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationResultStack

/**
 * Pipeline for validating multi-value property keys.
 * Extends base validation with multi-value restriction checks.
 *
 * Steps:
 * 1. Normalize property key (remove invalid chars, truncate)
 * 2. Validate normalized key
 * 3. Check multi-value restrictions
 * 4. Automatically report validation errors to the error reporter
 * 
 * @param config Validation configuration
 * @param errorReporter Error reporter for pushing errors to stack.
 *                      All validation errors are automatically pushed to this stack.
 */
class MultiValuePropertyKeyValidationPipeline(
    config: ValidationConfig,
    errorReporter: ValidationResultStack
) : EventPropertyKeyValidationPipeline(config, errorReporter) {
    
    override val validator = MultiValuePropertyKeyValidator(config)
}
