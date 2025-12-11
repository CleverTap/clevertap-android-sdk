package com.clevertap.android.sdk.validation.pipeline

import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.eventdata.EventDataValidationPipeline
import com.clevertap.android.sdk.validation.eventname.EventNameValidationPipeline
import com.clevertap.android.sdk.validation.propertykey.EventPropertyKeyValidationPipeline
import com.clevertap.android.sdk.validation.propertykey.MultiValuePropertyKeyValidationPipeline

/**
 * Provides access to all validation pipelines.
 * Simplifies dependency injection by bundling related pipelines together.
 * 
 * All pipelines are lazily initialized and reused throughout the lifecycle.
 */
class ValidationPipelineProvider(
    private val config: ValidationConfig
) {
    /**
     * Pipeline for validating event names.
     */
    val eventNamePipeline: EventNameValidationPipeline by lazy {
        EventNameValidationPipeline(config)
    }
    
    /**
     * Pipeline for validating event data (properties).
     */
    val eventDataPipeline: EventDataValidationPipeline by lazy {
        EventDataValidationPipeline(config)
    }
    
    /**
     * Pipeline for validating regular property keys.
     */
    val propertyKeyPipeline: EventPropertyKeyValidationPipeline by lazy {
        EventPropertyKeyValidationPipeline(config)
    }
    
    /**
     * Pipeline for validating multi-value property keys.
     */
    val multiValuePropertyKeyPipeline: MultiValuePropertyKeyValidationPipeline by lazy {
        MultiValuePropertyKeyValidationPipeline(config)
    }
}
