package com.clevertap.android.sdk.validation.pipeline

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.validation.chargedevent.ChargedEventItemsValidationPipeline
import com.clevertap.android.sdk.validation.eventdata.EventDataValidationPipeline
import com.clevertap.android.sdk.validation.eventname.EventNameValidationPipeline
import com.clevertap.android.sdk.validation.propertykey.EventPropertyKeyValidationPipeline
import com.clevertap.android.sdk.validation.eventdata.MultiValueDataValidationPipeline

/**
 * Provides access to all validation pipelines.
 * Simplifies dependency injection by bundling related pipelines together.
 * 
 * All pipelines are lazily initialized and reused throughout the lifecycle.
 * Validation errors are automatically reported to the provided error reporter.
 * 
 * @param errorReporter Error reporter for pushing errors to stack.
 *                      All validation pipelines will automatically push their errors to this stack.
 * @param logger
 */
class ValidationPipelineProvider(
    private val errorReporter: ValidationResultStack,
    private val logger: ILogger
) {
    /**
     * Pipeline for validating event names.
     */
    val eventNamePipeline: EventNameValidationPipeline by lazy {
        EventNameValidationPipeline(errorReporter, logger)
    }
    
    /**
     * Pipeline for validating event data (properties).
     */
    val eventDataPipeline: EventDataValidationPipeline by lazy {
        EventDataValidationPipeline(errorReporter, logger)
    }
    
    /**
     * Pipeline for validating regular property keys.
     */
    val propertyKeyPipeline: EventPropertyKeyValidationPipeline by lazy {
        EventPropertyKeyValidationPipeline(errorReporter, logger)
    }
    
    /**
     * Pipeline for validating multi-value property keys.
     */
    val multiValueDataPipeline: MultiValueDataValidationPipeline by lazy {
        MultiValueDataValidationPipeline(errorReporter, logger)
    }

    /**
     * Pipeline for validating charged event items.
     */
    val chargedEventItemsValidationPipeline: ChargedEventItemsValidationPipeline by lazy {
        ChargedEventItemsValidationPipeline(errorReporter, logger)
    }
}
