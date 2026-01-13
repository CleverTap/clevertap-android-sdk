package com.clevertap.android.sdk.validation.eventdata

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.validation.DropReason
import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationOutcome
import com.clevertap.android.sdk.validation.ValidationResultStack
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class MultiValueDataValidationPipelineTest {

    private lateinit var errorReporter: ValidationResultStack
    private lateinit var logger: ILogger
    private lateinit var pipeline: MultiValueDataValidationPipeline

    @Before
    fun setup() {
        errorReporter = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        pipeline = MultiValueDataValidationPipeline(errorReporter, logger)
    }

    @Test
    fun `execute drops event when all properties are removed`() {
        val input = mapOf("key" to null)
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute(input, config)

        assertTrue(result.outcome is ValidationOutcome.Drop)
        assertTrue(result.outcome.reason == DropReason.EMPTY_EVENT_DATA)
    }

    @Test
    fun `execute does not drop event when valid data remains`() {
        val input = mapOf("key1" to "value1", "key2" to null)
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute(input, config)

        assertTrue(result.outcome !is ValidationOutcome.Drop)
    }

    @Test
    fun `execute drops event when only empty keys exist`() {
        val input = mapOf("" to "value")
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute(input, config)

        assertTrue(result.outcome is ValidationOutcome.Drop)
    }

    @Test
    fun `execute inherits base validation behavior`() {
        val input = mapOf("key" to "a".repeat(600))
        val config = ValidationConfig.Builder()
            .addValueSizeValidation(500)
            .build()

        val result = pipeline.execute(input, config)

        // Should have warning for truncation but not dropped
        assertTrue(result.outcome is ValidationOutcome.Warning)
    }

    @Test
    fun `execute handles null input as empty data`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute(null, config)

        assertTrue(result.outcome is ValidationOutcome.Drop)
        assertTrue(result.outcome.reason == DropReason.EMPTY_EVENT_DATA)
    }

    @Test
    fun `execute drops when all values become empty after normalization`() {
        val input = mapOf("key" to "   ")
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute(input, config)

        assertTrue(result.outcome is ValidationOutcome.Drop)
    }
}
