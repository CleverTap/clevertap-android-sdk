package com.clevertap.android.sdk.validation.pipeline

import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationOutcome

/**
 * Base interface for all validators.
 * Validators evaluate normalized data against business rules and limits.
 *
 * @param I Input type to validate (typically a normalization result)
 */
interface Validator<I> {
    /**
     * Validates the input data.
     *
     * @param input The normalized data to validate
     * @param config Validation configuration to use for validation
     * @return ValidationOutcome indicating success, warning, or drop
     */
    fun validate(input: I, config: ValidationConfig): ValidationOutcome
}
