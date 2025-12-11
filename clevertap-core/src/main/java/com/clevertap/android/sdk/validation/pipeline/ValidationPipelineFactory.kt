package com.clevertap.android.sdk.validation.pipeline

import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.eventdata.EventDataValidationPipeline
import com.clevertap.android.sdk.validation.eventname.EventNameValidationPipeline
import com.clevertap.android.sdk.validation.propertykey.EventPropertyKeyValidationPipeline
import com.clevertap.android.sdk.validation.propertykey.MultiValuePropertyKeyValidationPipeline

/**
 * Factory for creating validation pipelines with different configurations.
 * Provides preset configurations for common use cases.
 */
object ValidationPipelineFactory {
    
    /**
     * Creates an event name validation pipeline.
     * Validates and normalizes event names, checking restrictions.
     */
    fun createEventNamePipeline(config: ValidationConfig): EventNameValidationPipeline {
        return EventNameValidationPipeline(
            config
        )
    }
    
    /**
     * Creates a property key validation pipeline.
     * Validates and normalizes property keys.
     */
    fun createPropertyKeyPipeline(config: ValidationConfig): EventPropertyKeyValidationPipeline {
        return EventPropertyKeyValidationPipeline(config)
    }
    
    /**
     * Creates a multi-value property key validation pipeline.
     * Validates and normalizes property keys with multi-value restriction checks.
     */
    fun createMultiValuePropertyKeyPipeline(config: ValidationConfig): MultiValuePropertyKeyValidationPipeline {
        return MultiValuePropertyKeyValidationPipeline(config)
    }
    
    /**
     * Creates an event data validation pipeline.
     * Validates and normalizes event property data including nested structures.
     */
    fun createEventDataPipeline(config: ValidationConfig): EventDataValidationPipeline {
        return EventDataValidationPipeline(
            config
        )
    }
}
