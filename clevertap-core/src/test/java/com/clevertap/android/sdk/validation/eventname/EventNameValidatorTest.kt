package com.clevertap.android.sdk.validation.eventname

import com.clevertap.android.sdk.validation.DropReason
import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationError
import com.clevertap.android.sdk.validation.ValidationOutcome
import com.clevertap.android.sdk.validation.pipeline.EventNameNormalizationResult
import com.clevertap.android.sdk.validation.pipeline.ModificationReason
import org.junit.Assert.*
import org.junit.Test

class EventNameValidatorTest {

    private val validator = EventNameValidator()

    @Test
    fun `validate returns Drop for null original name`() {
        val input = EventNameNormalizationResult(
            originalName = null,
            cleanedName = "",
            modifications = emptySet()
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Drop)
        assertEquals(DropReason.NULL_EVENT_NAME, (result as ValidationOutcome.Drop).reason)
        assertEquals(1, result.errors.size)
        assertEquals(ValidationError.EVENT_NAME_NULL.code, result.errors[0].errorCode)
    }

    @Test
    fun `validate returns Drop for empty cleaned name`() {
        val input = EventNameNormalizationResult(
            originalName = "   ",
            cleanedName = "",
            modifications = emptySet()
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Drop)
        assertEquals(DropReason.NULL_EVENT_NAME, (result as ValidationOutcome.Drop).reason)
        assertEquals(1, result.errors.size)
        assertEquals(ValidationError.EVENT_NAME_NULL.code, result.errors[0].errorCode)
    }

    @Test
    fun `validate returns Success for valid event name with no modifications`() {
        val input = EventNameNormalizationResult(
            originalName = "ValidEvent",
            cleanedName = "ValidEvent",
            modifications = emptySet()
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate returns Warning for truncated event name`() {
        val input = EventNameNormalizationResult(
            originalName = "VeryLongEventName",
            cleanedName = "VeryLongEv",
            modifications = setOf(ModificationReason.TRUNCATED_TO_MAX_LENGTH)
        )
        val config = ValidationConfig.Builder()
            .addEventNameLengthValidation(10)
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Warning)
        assertEquals(1, result.errors.size)
        assertEquals(ValidationError.EVENT_NAME_TOO_LONG.code, result.errors[0].errorCode)
    }

    @Test
    fun `validate returns Warning for invalid characters removed`() {
        val input = EventNameNormalizationResult(
            originalName = "Event!Name",
            cleanedName = "EventName",
            modifications = setOf(ModificationReason.INVALID_CHARACTERS_REMOVED)
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Warning)
        assertEquals(1, result.errors.size)
        assertEquals(ValidationError.EVENT_NAME_INVALID_CHARACTERS.code, result.errors[0].errorCode)
    }

    @Test
    fun `validate returns Warning for both truncation and invalid characters`() {
        val input = EventNameNormalizationResult(
            originalName = "VeryLong!Event@Name",
            cleanedName = "VeryLongEv",
            modifications = setOf(
                ModificationReason.TRUNCATED_TO_MAX_LENGTH,
                ModificationReason.INVALID_CHARACTERS_REMOVED
            )
        )
        val config = ValidationConfig.Builder()
            .addEventNameLengthValidation(10)
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Warning)
        assertEquals(2, result.errors.size)
        val errorCodes = result.errors.map { it.errorCode }.toSet()
        assertTrue(errorCodes.contains(ValidationError.EVENT_NAME_TOO_LONG.code))
        assertTrue(errorCodes.contains(ValidationError.EVENT_NAME_INVALID_CHARACTERS.code))
    }

    @Test
    fun `validate returns Drop for restricted event name`() {
        val input = EventNameNormalizationResult(
            originalName = "Restricted",
            cleanedName = "Restricted",
            modifications = emptySet()
        )
        val config = ValidationConfig.Builder()
            .setRestrictedEventNames(setOf("Restricted", "Blocked"))
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Drop)
        assertEquals(DropReason.RESTRICTED_EVENT_NAME, (result as ValidationOutcome.Drop).reason)
        assertEquals(1, result.errors.size)
        assertEquals(ValidationError.RESTRICTED_EVENT_NAME.code, result.errors[0].errorCode)
    }

    @Test
    fun `validate returns Drop for case-insensitive restricted event name`() {
        val input = EventNameNormalizationResult(
            originalName = "RESTRICTED",
            cleanedName = "RESTRICTED",
            modifications = emptySet()
        )
        val config = ValidationConfig.Builder()
            .setRestrictedEventNames(setOf("restricted"))
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Drop)
        assertEquals(DropReason.RESTRICTED_EVENT_NAME, (result as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `validate returns Success when event name not in restricted list`() {
        val input = EventNameNormalizationResult(
            originalName = "AllowedEvent",
            cleanedName = "AllowedEvent",
            modifications = emptySet()
        )
        val config = ValidationConfig.Builder()
            .setRestrictedEventNames(setOf("Restricted", "Blocked"))
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate returns Drop for discarded event name`() {
        val input = EventNameNormalizationResult(
            originalName = "DiscardedEvent",
            cleanedName = "DiscardedEvent",
            modifications = emptySet()
        )
        val config = ValidationConfig.Builder()
            .setDiscardedEventNames(setOf("DiscardedEvent", "Ignored"))
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Drop)
        assertEquals(DropReason.DISCARDED_EVENT_NAME, (result as ValidationOutcome.Drop).reason)
        assertEquals(1, result.errors.size)
        assertEquals(ValidationError.DISCARDED_EVENT_NAME.code, result.errors[0].errorCode)
    }

    @Test
    fun `validate returns Drop for case-insensitive discarded event name`() {
        val input = EventNameNormalizationResult(
            originalName = "DISCARDED",
            cleanedName = "DISCARDED",
            modifications = emptySet()
        )
        val config = ValidationConfig.Builder()
            .setDiscardedEventNames(setOf("discarded"))
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Drop)
        assertEquals(DropReason.DISCARDED_EVENT_NAME, (result as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `validate returns Success when event name not in discarded list`() {
        val input = EventNameNormalizationResult(
            originalName = "AllowedEvent",
            cleanedName = "AllowedEvent",
            modifications = emptySet()
        )
        val config = ValidationConfig.Builder()
            .setDiscardedEventNames(setOf("Discarded", "Ignored"))
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate prioritizes restricted over discarded`() {
        val input = EventNameNormalizationResult(
            originalName = "BadEvent",
            cleanedName = "BadEvent",
            modifications = emptySet()
        )
        val config = ValidationConfig.Builder()
            .setRestrictedEventNames(setOf("BadEvent"))
            .setDiscardedEventNames(setOf("BadEvent"))
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Drop)
        assertEquals(DropReason.RESTRICTED_EVENT_NAME, (result as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `validate includes modification warnings with Drop for restricted name`() {
        val input = EventNameNormalizationResult(
            originalName = "Restricted!Event",
            cleanedName = "RestrictedEvent",
            modifications = setOf(ModificationReason.INVALID_CHARACTERS_REMOVED)
        )
        val config = ValidationConfig.Builder()
            .setRestrictedEventNames(setOf("RestrictedEvent"))
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Drop)
        assertEquals(DropReason.RESTRICTED_EVENT_NAME, (result as ValidationOutcome.Drop).reason)
        // Should have both modification warning and restriction error
        assertEquals(2, result.errors.size)
        val errorCodes = result.errors.map { it.errorCode }.toSet()
        assertTrue(errorCodes.contains(ValidationError.EVENT_NAME_INVALID_CHARACTERS.code))
        assertTrue(errorCodes.contains(ValidationError.RESTRICTED_EVENT_NAME.code))
    }

    @Test
    fun `validate includes modification warnings with Drop for discarded name`() {
        val input = EventNameNormalizationResult(
            originalName = "VeryLongDiscardedEventName",
            cleanedName = "VeryLongDi",
            modifications = setOf(ModificationReason.TRUNCATED_TO_MAX_LENGTH)
        )
        val config = ValidationConfig.Builder()
            .addEventNameLengthValidation(10)
            .setDiscardedEventNames(setOf("VeryLongDi"))
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Drop)
        assertEquals(DropReason.DISCARDED_EVENT_NAME, (result as ValidationOutcome.Drop).reason)
        // Should have both truncation warning and discard error
        assertEquals(2, result.errors.size)
        val errorCodes = result.errors.map { it.errorCode }.toSet()
        assertTrue(errorCodes.contains(ValidationError.EVENT_NAME_TOO_LONG.code))
        assertTrue(errorCodes.contains(ValidationError.DISCARDED_EVENT_NAME.code))
    }

    @Test
    fun `validate with null restricted names list allows all names`() {
        val input = EventNameNormalizationResult(
            originalName = "AnyEvent",
            cleanedName = "AnyEvent",
            modifications = emptySet()
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate with null discarded names list allows all names`() {
        val input = EventNameNormalizationResult(
            originalName = "AnyEvent",
            cleanedName = "AnyEvent",
            modifications = emptySet()
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate with empty restricted names set allows all names`() {
        val input = EventNameNormalizationResult(
            originalName = "AnyEvent",
            cleanedName = "AnyEvent",
            modifications = emptySet()
        )
        val config = ValidationConfig.Builder()
            .setRestrictedEventNames(emptySet())
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate with empty discarded names set allows all names`() {
        val input = EventNameNormalizationResult(
            originalName = "AnyEvent",
            cleanedName = "AnyEvent",
            modifications = emptySet()
        )
        val config = ValidationConfig.Builder()
            .setDiscardedEventNames(emptySet())
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate handles event name with leading and trailing spaces after normalization`() {
        val input = EventNameNormalizationResult(
            originalName = "  Event  ",
            cleanedName = "Event",
            modifications = emptySet()
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate checks restricted name against cleaned name not original`() {
        val input = EventNameNormalizationResult(
            originalName = "Event!Name",
            cleanedName = "EventName",
            modifications = setOf(ModificationReason.INVALID_CHARACTERS_REMOVED)
        )
        val config = ValidationConfig.Builder()
            .setRestrictedEventNames(setOf("EventName"))
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Drop)
        assertEquals(DropReason.RESTRICTED_EVENT_NAME, (result as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `validate checks discarded name against cleaned name not original`() {
        val input = EventNameNormalizationResult(
            originalName = "VeryLongEventName",
            cleanedName = "VeryLongEv",
            modifications = setOf(ModificationReason.TRUNCATED_TO_MAX_LENGTH)
        )
        val config = ValidationConfig.Builder()
            .addEventNameLengthValidation(10)
            .setDiscardedEventNames(setOf("VeryLongEv"))
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Drop)
        assertEquals(DropReason.DISCARDED_EVENT_NAME, (result as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `validate records modifications before checking empty cleaned name`() {
        val input = EventNameNormalizationResult(
            originalName = "!@#",
            cleanedName = "",
            modifications = setOf(ModificationReason.INVALID_CHARACTERS_REMOVED)
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Drop)
        assertEquals(DropReason.NULL_EVENT_NAME, (result as ValidationOutcome.Drop).reason)
        // Should have both modification warning and null name error
        assertEquals(2, result.errors.size)
        val errorCodes = result.errors.map { it.errorCode }.toSet()
        assertTrue(errorCodes.contains(ValidationError.EVENT_NAME_INVALID_CHARACTERS.code))
        assertTrue(errorCodes.contains(ValidationError.EVENT_NAME_NULL.code))
    }

    @Test
    fun `validate handles unicode event names`() {
        val input = EventNameNormalizationResult(
            originalName = "事件名称",
            cleanedName = "事件名称",
            modifications = emptySet()
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate handles numbers in event names`() {
        val input = EventNameNormalizationResult(
            originalName = "Event123",
            cleanedName = "Event123",
            modifications = emptySet()
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate handles mixed case event names`() {
        val input = EventNameNormalizationResult(
            originalName = "EventName",
            cleanedName = "EventName",
            modifications = emptySet()
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }
}
