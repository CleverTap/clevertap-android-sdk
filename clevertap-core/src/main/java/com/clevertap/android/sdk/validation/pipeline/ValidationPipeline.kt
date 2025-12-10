package com.clevertap.android.sdk.validation.pipeline


/**
 * Base interface for all validation pipelines.
 * Defines the contract for processing and validating different types of data.
 *
 * @param I Input type to validate
 * @param O Output type containing validation results
 */
interface ValidationPipeline<I, O> {
    /**
     * Executes the validation pipeline for the given input.
     *
     * @param input The data to validate
     * @return The validation result containing outcome and cleaned data
     */
    fun execute(input: I): O
}
