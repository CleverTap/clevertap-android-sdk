package com.clevertap.android.sdk.validation.pipeline

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.validation.ValidationConfig

/**
 * Base interface for all normalizers.
 * Normalizers transform and clean data according to validation rules.
 *
 * @param I Input type to normalize
 * @param O Output type after normalization
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface Normalizer<I, O> {
    /**
     * Normalizes the input data.
     *
     * @param input The data to normalize
     * @param config Validation configuration to use for normalization
     * @return Normalization result with cleaned data and metrics
     */
    fun normalize(input: I, config: ValidationConfig): O
}
