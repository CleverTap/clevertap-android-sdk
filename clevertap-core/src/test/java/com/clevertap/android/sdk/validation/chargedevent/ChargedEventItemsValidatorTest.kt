package com.clevertap.android.sdk.validation.chargedevent

import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationError
import com.clevertap.android.sdk.validation.ValidationOutcome
import com.clevertap.android.sdk.validation.pipeline.ChargedEventItemsNormalizationResult
import org.junit.Assert.*
import org.junit.Test

class ChargedEventItemsValidatorTest {

    private val validator = ChargedEventItemsValidator()

    @Test
    fun `validate returns Success for zero items`() {
        val input = ChargedEventItemsNormalizationResult(itemsCount = 0)
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate returns Success when count is below limit`() {
        val input = ChargedEventItemsNormalizationResult(itemsCount = 25)
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate returns Success when count equals limit`() {
        val input = ChargedEventItemsNormalizationResult(itemsCount = 50)
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate returns Warning when count exceeds limit`() {
        val input = ChargedEventItemsNormalizationResult(itemsCount = 51)
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Warning)
        assertEquals(1, result.errors.size)
        assertEquals(ValidationError.CHARGED_EVENT_TOO_MANY_ITEMS.code, result.errors[0].errorCode)
    }

    @Test
    fun `validate returns Warning when count far exceeds limit`() {
        val input = ChargedEventItemsNormalizationResult(itemsCount = 100)
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Warning)
        assertEquals(1, result.errors.size)
        assertEquals(ValidationError.CHARGED_EVENT_TOO_MANY_ITEMS.code, result.errors[0].errorCode)
    }

    @Test
    fun `validate returns Warning when count slightly exceeds limit`() {
        val input = ChargedEventItemsNormalizationResult(itemsCount = 51)
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Warning)
        assertEquals(1, result.errors.size)
    }

    @Test
    fun `validate returns Success when no max count configured`() {
        val input = ChargedEventItemsNormalizationResult(itemsCount = 1000)
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate never returns Drop outcome`() {
        val input = ChargedEventItemsNormalizationResult(itemsCount = 1000)
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()

        val result = validator.validate(input, config)

        assertFalse(result is ValidationOutcome.Drop)
        assertTrue(result is ValidationOutcome.Warning || result is ValidationOutcome.Success)
    }

    @Test
    fun `validate with different max count limits`() {
        val input = ChargedEventItemsNormalizationResult(itemsCount = 30)

        // Test with limit 50 - should pass
        val config1 = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()
        val result1 = validator.validate(input, config1)
        assertTrue(result1 is ValidationOutcome.Success)

        // Test with limit 20 - should warn
        val config2 = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(20)
            .build()
        val result2 = validator.validate(input, config2)
        assertTrue(result2 is ValidationOutcome.Warning)
    }

    @Test
    fun `validate with zero max count warns for any items`() {
        val input = ChargedEventItemsNormalizationResult(itemsCount = 1)
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(0)
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Warning)
        assertEquals(1, result.errors.size)
    }

    @Test
    fun `validate with zero items and zero max count is Success`() {
        val input = ChargedEventItemsNormalizationResult(itemsCount = 0)
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(0)
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate with very large max count`() {
        val input = ChargedEventItemsNormalizationResult(itemsCount = 100)
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(10000)
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate boundary case exactly one over limit`() {
        val input = ChargedEventItemsNormalizationResult(itemsCount = 51)
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Warning)
        assertEquals(1, result.errors.size)
    }

    @Test
    fun `validate boundary case exactly at limit`() {
        val input = ChargedEventItemsNormalizationResult(itemsCount = 50)
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate boundary case exactly one under limit`() {
        val input = ChargedEventItemsNormalizationResult(itemsCount = 49)
        val config = ValidationConfig.Builder()
            .addChargedEventItemsCountValidation(50)
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate with default config limit of 50`() {
        val input = ChargedEventItemsNormalizationResult(itemsCount = 60)
        val config = ValidationConfig.default().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Warning)
        assertEquals(1, result.errors.size)
    }
}
