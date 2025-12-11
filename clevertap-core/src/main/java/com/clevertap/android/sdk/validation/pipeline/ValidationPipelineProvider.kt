package com.clevertap.android.sdk.validation.pipeline

import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.sdk.validation.eventdata.EventDataValidationPipeline
import com.clevertap.android.sdk.validation.eventname.EventNameValidationPipeline
import com.clevertap.android.sdk.validation.propertykey.EventPropertyKeyValidationPipeline
import com.clevertap.android.sdk.validation.propertykey.MultiValuePropertyKeyValidationPipeline

/**
 * Provides access to all validation pipelines.
 * Simplifies dependency injection by bundling related pipelines together.
 * 
 * All pipelines are lazily initialized and reused throughout the lifecycle.
 * 
 * @param config Validation configuration
 * @param errorReporter Optional error reporter for automatic error logging.
 *                      If provided, pipelines will automatically push validation errors.
 *                      Pass null to disable automatic error reporting (useful for testing).
 */
class ValidationPipelineProvider(
    private val config: ValidationConfig,
    private val errorReporter: ValidationResultStack
) {
    /**
     * Pipeline for validating event names.
     */
    val eventNamePipeline: EventNameValidationPipeline by lazy {
        EventNameValidationPipeline(config, errorReporter)
    }
    
    /**
     * Pipeline for validating event data (properties).
     */
    val eventDataPipeline: EventDataValidationPipeline by lazy {
        EventDataValidationPipeline(config, errorReporter)
    }
    
    /**
     * Pipeline for validating regular property keys.
     */
    val propertyKeyPipeline: EventPropertyKeyValidationPipeline by lazy {
        EventPropertyKeyValidationPipeline(config, errorReporter)
    }
    
    /**
     * Pipeline for validating multi-value property keys.
     */
    val multiValuePropertyKeyPipeline: MultiValuePropertyKeyValidationPipeline by lazy {
        MultiValuePropertyKeyValidationPipeline(config, errorReporter)
    }
}
