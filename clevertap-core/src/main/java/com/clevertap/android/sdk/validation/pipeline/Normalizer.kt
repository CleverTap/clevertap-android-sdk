package com.clevertap.android.sdk.validation.pipeline

/**
 * Base interface for all normalizers.
 * Normalizers transform and clean data according to validation rules.
 *
 * @param I Input type to normalize
 * @param O Output type after normalization
 */
interface Normalizer<I, O> {
    /**
     * Normalizes the input data.
     *
     * @param input The data to normalize
     * @return Normalization result with cleaned data and metrics
     */
    fun normalize(input: I): O
}
