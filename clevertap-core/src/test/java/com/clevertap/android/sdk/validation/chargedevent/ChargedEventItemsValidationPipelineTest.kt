package com.clevertap.android.sdk.validation.chargedevent

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationOutcome
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.ValidationResultStack
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ChargedEventItemsValidationPipelineTest {

    private lateinit var errorReporter: ValidationResultStack
    private lateinit var logger: ILogger
    private lateinit var pipeline: ChargedEventItemsValidationPipeline

    @Before
    fun setup() {
        errorReporter = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        pipeline = ChargedEventItemsValidationPipeline(errorReporter, logger)
    }

    @Test
    fun `execute returns zero count for null input`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute(null, config)

        assertEquals(0, result.itemsCount)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute returns zero count for empty list`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute(emptyList<Any>(), config)

        assertEquals(0, result.itemsCount)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute counts single item`() {
        val config = ValidationConfig.Builder().build()
        val items = listOf(mapOf("name" to "Item1"))

        val result = pipeline.execute(items, config)

        assertEquals(1, result.itemsCount)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute counts multiple items`() {
        val config = ValidationConfig.Builder().build()
        val items = listOf(
            mapOf("name" to "Item1"),
            mapOf("name" to "Item2"),
            mapOf("name" to "Item3")
        )

        val result = pipeline.execute(items, config)

        assertEquals(3, result.itemsCount)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute returns Success when below limit`() {
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()
        val items = (1..30).map { mapOf("id" to it) }

        val result = pipeline.execute(items, config)

        assertEquals(30, result.itemsCount)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute returns Success when at limit`() {
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()
        val items = (1..50).map { mapOf("id" to it) }

        val result = pipeline.execute(items, config)

        assertEquals(50, result.itemsCount)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute returns Warning when exceeding limit`() {
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()
        val items = (1..60).map { mapOf("id" to it) }

        val result = pipeline.execute(items, config)

        assertEquals(60, result.itemsCount)
        assertTrue(result.outcome is ValidationOutcome.Warning)
        assertEquals(1, result.outcome.errors.size)
    }

    @Test
    fun `execute pushes validation errors to error reporter`() {
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()
        val items = (1..60).map { mapOf("id" to it) }

        pipeline.execute(items, config)

        verify { errorReporter.pushValidationResult(any<List<ValidationResult>>()) }
    }

    @Test
    fun `execute pushes empty errors for success`() {
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()
        val items = (1..30).map { mapOf("id" to it) }

        pipeline.execute(items, config)

        verify { errorReporter.pushValidationResult(match<List<ValidationResult>> { it.isEmpty() }) }
    }

    @Test
    fun `execute pushes one error when exceeding limit`() {
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()
        val items = (1..60).map { mapOf("id" to it) }

        pipeline.execute(items, config)

        verify { errorReporter.pushValidationResult(match<List<ValidationResult>> { it.size == 1 }) }
    }

    @Test
    fun `execute never drops events`() {
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()

        // Test with way over limit
        val items1 = (1..1000).map { mapOf("id" to it) }
        val result1 = pipeline.execute(items1, config)
        assertFalse(result1.shouldDrop())

        // Test with zero items
        val result2 = pipeline.execute(emptyList<Any>(), config)
        assertFalse(result2.shouldDrop())

        // Test with null
        val result3 = pipeline.execute(null, config)
        assertFalse(result3.shouldDrop())
    }

    @Test
    fun `execute handles list with different types`() {
        val config = ValidationConfig.Builder().build()
        val items = listOf(
            mapOf("name" to "Item1"),
            "StringItem",
            123,
            true,
            null
        )

        val result = pipeline.execute(items, config)

        assertEquals(5, result.itemsCount)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute with no max count configured always succeeds`() {
        val config = ValidationConfig.Builder().build()
        val items = (1..1000).map { mapOf("id" to it) }

        val result = pipeline.execute(items, config)

        assertEquals(1000, result.itemsCount)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute with default config validates against 50 items`() {
        val config = ValidationConfig.default().build()

        // Below limit
        val items1 = (1..30).map { mapOf("id" to it) }
        val result1 = pipeline.execute(items1, config)
        assertTrue(result1.outcome is ValidationOutcome.Success)

        // Above limit
        val items2 = (1..60).map { mapOf("id" to it) }
        val result2 = pipeline.execute(items2, config)
        assertTrue(result2.outcome is ValidationOutcome.Warning)
    }

    @Test
    fun `execute boundary test exactly 51 items`() {
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()
        val items = (1..51).map { mapOf("id" to it) }

        val result = pipeline.execute(items, config)

        assertEquals(51, result.itemsCount)
        assertTrue(result.outcome is ValidationOutcome.Warning)
    }

    @Test
    fun `execute boundary test exactly 49 items`() {
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()
        val items = (1..49).map { mapOf("id" to it) }

        val result = pipeline.execute(items, config)

        assertEquals(49, result.itemsCount)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute with zero max count`() {
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(0)
            .build()

        // Zero items should succeed
        val result1 = pipeline.execute(emptyList<Any>(), config)
        assertTrue(result1.outcome is ValidationOutcome.Success)

        // One item should warn
        val result2 = pipeline.execute(listOf(mapOf("id" to 1)), config)
        assertTrue(result2.outcome is ValidationOutcome.Warning)
    }

    @Test
    fun `execute with very large items list`() {
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()
        val items = (1..10000).map { mapOf("id" to it) }

        val result = pipeline.execute(items, config)

        assertEquals(10000, result.itemsCount)
        assertTrue(result.outcome is ValidationOutcome.Warning)
    }

    @Test
    fun `execute handles list with nested structures`() {
        val config = ValidationConfig.Builder().build()
        val items = listOf(
            mapOf("name" to "Item1", "details" to mapOf("color" to "red", "size" to "large")),
            mapOf("name" to "Item2", "tags" to listOf("new", "sale"))
        )

        val result = pipeline.execute(items, config)

        assertEquals(2, result.itemsCount)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }
}
