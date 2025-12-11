package com.clevertap.android.sdk.validation.propertykey

import com.clevertap.android.sdk.validation.ValidationConfig

/**
 * Pipeline for validating multi-value property keys.
 * Extends base validation with multi-value restriction checks.
 *
 * Steps:
 * 1. Normalize property key (remove invalid chars, truncate)
 * 2. Validate normalized key
 * 3. Check multi-value restrictions
 */
class MultiValuePropertyKeyValidationPipeline(
    config: ValidationConfig
) : EventPropertyKeyValidationPipeline(config) {
    
    override val validator = MultiValuePropertyKeyValidator(config)
}
