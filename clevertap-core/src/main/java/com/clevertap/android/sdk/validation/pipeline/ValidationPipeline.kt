package com.clevertap.android.sdk.validation.pipeline

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.validation.ValidationOutcome

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
    
    /**
     * Logs validation outcome
     * Provides consistent logging across all pipeline implementations.
     * 
     * @param logger Logger instance to use
     * @param tag Log tag (e.g., "EventNameValidation")
     * @param outcome Validation outcome to log
     */
    fun logValidationOutcome(
        logger: ILogger,
        tag: String,
        outcome: ValidationOutcome,
    ) {
        when (outcome) {
            is ValidationOutcome.Drop -> {
                logger.verbose(
                    tag,
                    "Dropped. Reason: ${outcome.reason}"
                )
                outcome.errors.forEach { error ->
                    logger.verbose(tag, "${error.errorCode}: ${error.errorDesc}")                }
            }
            is ValidationOutcome.Warning -> {
                outcome.errors.forEach { error ->
                    logger.verbose(tag, "${error.errorCode}: ${error.errorDesc}")                }
            }
            is ValidationOutcome.Success -> {
                // No logging for successful validation by default
                // Subclasses can override to add custom success logging
            }
        }
    }
}
