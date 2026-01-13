package com.clevertap.android.sdk.validation.eventdata

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationOutcome
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.ValidationResultStack
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class EventDataValidationPipelineTest {

    private lateinit var errorReporter: ValidationResultStack
    private lateinit var logger: ILogger
    private lateinit var pipeline: EventDataValidationPipeline

    @Before
    fun setup() {
        errorReporter = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        pipeline = EventDataValidationPipeline(errorReporter, logger)
    }

    @Test
    fun `execute normalizes and validates input`() {
        val input = mapOf("key" to "value")
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute(input, config)

        assertEquals("value", result.cleanedData.getString("key"))
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute pushes validation errors to error reporter`() {
        val input = mapOf("key$" to "value")
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('$'))
            .build()

        pipeline.execute(input, config)

        verify { errorReporter.pushValidationResult(any<List<ValidationResult>>()) }
    }

    @Test
    fun `execute handles null input`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute(null, config)

        assertEquals(0, result.cleanedData.length())
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute normalizes and reports warnings for limit violations`() {
        val input = mapOf(
            "level1" to mapOf(
                "level2" to mapOf(
                    "level3" to mapOf(
                        "level4" to mapOf(
                            "level5" to mapOf(
                                "level6" to mapOf(
                                    "level7" to mapOf(
                                        "level8" to mapOf(
                                            "level9" to mapOf(
                                                "level10" to mapOf(
                                                    "level11" to "deep"
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )
        val config = ValidationConfig.Builder()
            .addDepthValidation(5)
            .build()

        val result = pipeline.execute(input, config)

        assertTrue(result.outcome is ValidationOutcome.Warning)
        verify { errorReporter.pushValidationResult(match<List<ValidationResult>> { it.isNotEmpty() }) }
    }

    @Test
    fun `execute returns cleaned data even with warnings`() {
        val input = mapOf("key" to "a".repeat(600))
        val config = ValidationConfig.Builder()
            .addValueSizeValidation(500)
            .build()

        val result = pipeline.execute(input, config)

        assertEquals(500, result.cleanedData.getString("key").length)
        assertTrue(result.outcome is ValidationOutcome.Warning)
    }

    @Test
    fun `execute handles complex nested structures`() {
        val input = mapOf(
            "array" to listOf(1, 2, 3),
            "object" to mapOf("nested" to "value"),
            "primitive" to 42
        )
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute(input, config)

        assertEquals(3, result.cleanedData.getJSONArray("array").length())
        assertEquals("value", result.cleanedData.getJSONObject("object").getString("nested"))
        assertEquals(42, result.cleanedData.getInt("primitive"))
    }

    @Test
    fun `execute removes restricted multi-value fields and reports warnings`() {
        val input = mapOf(
            "email" to mapOf("nested" to "value"),
            "phone" to "123456"
        )
        val config = ValidationConfig.Builder()
            .setRestrictedMultiValueFields(setOf("email"))
            .build()

        val result = pipeline.execute(input, config)

        assertEquals(1, result.cleanedData.length())
        assertTrue(result.cleanedData.has("phone"))
        verify { errorReporter.pushValidationResult(match<List<ValidationResult>> {  it.isNotEmpty() }) }
    }
}
