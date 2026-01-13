package com.clevertap.android.sdk.validation.eventname

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.validation.DropReason
import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationOutcome
import com.clevertap.android.sdk.validation.ValidationResult
import com.clevertap.android.sdk.validation.ValidationResultStack
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class EventNameValidationPipelineTest {

    private lateinit var errorReporter: ValidationResultStack
    private lateinit var logger: ILogger
    private lateinit var pipeline: EventNameValidationPipeline

    @Before
    fun setup() {
        errorReporter = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        pipeline = EventNameValidationPipeline(errorReporter, logger)
    }

    @Test
    fun `execute returns empty string for null input`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute(null, config)

        assertEquals("", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Drop)
        assertEquals(DropReason.NULL_EVENT_NAME, (result.outcome as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `execute pushes validation errors to error reporter`() {
        val config = ValidationConfig.Builder().build()

        pipeline.execute(null, config)

        verify { errorReporter.pushValidationResult(any<List<ValidationResult>>()) }
    }

    @Test
    fun `execute returns cleaned name for valid input`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("ValidEvent", config)

        assertEquals("ValidEvent", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute trims whitespace from input`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("  Event  ", config)

        assertEquals("Event", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute removes disallowed characters`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('!', '@', '#'))
            .build()

        val result = pipeline.execute("Event!Name@Test#", config)

        assertEquals("EventNameTest", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Warning)
    }

    @Test
    fun `execute truncates to max length`() {
        val config = ValidationConfig.Builder()
            .addEventNameLengthValidation(10)
            .build()

        val result = pipeline.execute("VeryLongEventName", config)

        assertEquals("VeryLongEv", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Warning)
    }

    @Test
    fun `execute returns Drop for restricted event name`() {
        val config = ValidationConfig.Builder()
            .setRestrictedEventNames(setOf("Restricted"))
            .build()

        val result = pipeline.execute("Restricted", config)

        assertEquals("Restricted", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Drop)
        assertEquals(DropReason.RESTRICTED_EVENT_NAME, (result.outcome as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `execute returns Drop for discarded event name`() {
        val config = ValidationConfig.Builder()
            .setDiscardedEventNames(setOf("Discarded"))
            .build()

        val result = pipeline.execute("Discarded", config)

        assertEquals("Discarded", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Drop)
        assertEquals(DropReason.DISCARDED_EVENT_NAME, (result.outcome as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `execute normalizes then validates`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('!'))
            .setRestrictedEventNames(setOf("RestrictedEvent"))
            .build()

        val result = pipeline.execute("Restricted!Event", config)

        // Should normalize first (remove !) then check restriction
        assertEquals("RestrictedEvent", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Drop)
        assertEquals(DropReason.RESTRICTED_EVENT_NAME, (result.outcome as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `execute returns Drop for empty cleaned name after normalization`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('!', '@', '#'))
            .build()

        val result = pipeline.execute("!@#", config)

        assertEquals("", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Drop)
        assertEquals(DropReason.NULL_EVENT_NAME, (result.outcome as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `execute handles comprehensive scenario with multiple modifications`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('!', '@'))
            .addEventNameLengthValidation(15)
            .build()

        val result = pipeline.execute("Very!Long@EventName", config)

        assertEquals("VeryLongEventNa", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Warning)
        assertEquals(2, result.outcome.errors.size)
    }

    @Test
    fun `execute reports all validation errors`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('!'))
            .addEventNameLengthValidation(10)
            .build()

        pipeline.execute("VeryLong!EventName", config)

        verify { errorReporter.pushValidationResult(match<List<ValidationResult>> { it.size == 2 }) }
    }

    @Test
    fun `execute returns cleaned name even for dropped events`() {
        val config = ValidationConfig.Builder()
            .setRestrictedEventNames(setOf("Restricted"))
            .build()

        val result = pipeline.execute("Restricted", config)

        // Even though dropped, cleaned name is returned
        assertEquals("Restricted", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Drop)
    }

    @Test
    fun `execute handles empty string input`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("", config)

        assertEquals("", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Drop)
        assertEquals(DropReason.NULL_EVENT_NAME, (result.outcome as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `execute handles whitespace-only input`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("   ", config)

        assertEquals("", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Drop)
        assertEquals(DropReason.NULL_EVENT_NAME, (result.outcome as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `execute with no config restrictions returns Success`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("ValidEvent", config)

        assertEquals("ValidEvent", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Success)
        assertTrue(result.outcome.errors.isEmpty())
    }

    @Test
    fun `execute with case-insensitive restricted name matching`() {
        val config = ValidationConfig.Builder()
            .setRestrictedEventNames(setOf("restricted"))
            .build()

        val result = pipeline.execute("RESTRICTED", config)

        assertEquals("RESTRICTED", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Drop)
        assertEquals(DropReason.RESTRICTED_EVENT_NAME, (result.outcome as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `execute with case-insensitive discarded name matching`() {
        val config = ValidationConfig.Builder()
            .setDiscardedEventNames(setOf("discarded"))
            .build()

        val result = pipeline.execute("DISCARDED", config)

        assertEquals("DISCARDED", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Drop)
        assertEquals(DropReason.DISCARDED_EVENT_NAME, (result.outcome as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `execute handles unicode characters`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("事件名称", config)

        assertEquals("事件名称", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute removes unicode characters if disallowed`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('事', '件'))
            .build()

        val result = pipeline.execute("事件名称", config)

        assertEquals("名称", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Warning)
    }

    @Test
    fun `execute handles special characters`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('\n', '\t', '\r'))
            .build()

        val result = pipeline.execute("Event\nName\tTest\r", config)

        assertEquals("EventNameTest", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Warning)
    }

    @Test
    fun `execute with zero max length truncates to empty and drops`() {
        val config = ValidationConfig.Builder()
            .addEventNameLengthValidation(0)
            .build()

        val result = pipeline.execute("Event", config)

        assertEquals("", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Drop)
        assertEquals(DropReason.NULL_EVENT_NAME, (result.outcome as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `execute handles numbers in event names`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("Event123", config)

        assertEquals("Event123", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute handles mixed case event names`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("EventName", config)

        assertEquals("EventName", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute preserves spaces within event name`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("Event Name Test", config)

        assertEquals("Event Name Test", result.cleanedName)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute handles complex scenario with all validations`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('!', '@', '#'))
            .addEventNameLengthValidation(20)
            .setRestrictedEventNames(setOf("BlockedEvent"))
            .setDiscardedEventNames(setOf("IgnoredEvent"))
            .build()

        // Test 1: Clean event
        val result1 = pipeline.execute("CleanEvent", config)
        assertEquals("CleanEvent", result1.cleanedName)
        assertTrue(result1.outcome is ValidationOutcome.Success)

        // Test 2: Event with modifications
        val result2 = pipeline.execute("Event!With@Invalid#Chars", config)
        assertEquals("EventWithInvalidChar", result2.cleanedName)
        assertTrue(result2.outcome is ValidationOutcome.Warning)

        // Test 3: Restricted event
        val result3 = pipeline.execute("BlockedEvent", config)
        assertEquals("BlockedEvent", result3.cleanedName)
        assertTrue(result3.outcome is ValidationOutcome.Drop)
        assertEquals(DropReason.RESTRICTED_EVENT_NAME, (result3.outcome as ValidationOutcome.Drop).reason)

        // Test 4: Discarded event
        val result4 = pipeline.execute("IgnoredEvent", config)
        assertEquals("IgnoredEvent", result4.cleanedName)
        assertTrue(result4.outcome is ValidationOutcome.Drop)
        assertEquals(DropReason.DISCARDED_EVENT_NAME, (result4.outcome as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `execute outcome shouldDrop returns correct value`() {
        val restrictedConfig = ValidationConfig.Builder()
            .setRestrictedEventNames(setOf("Blocked"))
            .build()

        // Test Drop outcome
        val dropResult = pipeline.execute("Blocked", restrictedConfig)
        assertTrue(dropResult.shouldDrop())

        // Test Success outcome
        val successResult = pipeline.execute("Allowed", ValidationConfig.Builder().build())
        assertFalse(successResult.shouldDrop())

        // Test Warning outcome
        val warningConfig = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('!'))
            .build()
        val warningResult = pipeline.execute("Event!", warningConfig)
        assertFalse(warningResult.shouldDrop())
    }
}
