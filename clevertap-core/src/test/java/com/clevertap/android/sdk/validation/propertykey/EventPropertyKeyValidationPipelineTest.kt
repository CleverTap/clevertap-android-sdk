package com.clevertap.android.sdk.validation.propertykey

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

class EventPropertyKeyValidationPipelineTest {

    private lateinit var errorReporter: ValidationResultStack
    private lateinit var logger: ILogger
    private lateinit var pipeline: EventPropertyKeyValidationPipeline

    @Before
    fun setup() {
        errorReporter = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        pipeline = EventPropertyKeyValidationPipeline(errorReporter, logger)
    }

    @Test
    fun `execute returns empty string for null input`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute(null, config)

        assertEquals("", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Drop)
        assertEquals(DropReason.EMPTY_KEY, (result.outcome as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `execute pushes validation errors to error reporter`() {
        val config = ValidationConfig.Builder().build()

        pipeline.execute(null, config)

        verify { errorReporter.pushValidationResult(any<List<ValidationResult>>()) }
    }

    @Test
    fun `execute returns cleaned key for valid input`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("ValidKey", config)

        assertEquals("ValidKey", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute trims whitespace from input`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("  PropertyKey  ", config)

        assertEquals("PropertyKey", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute removes disallowed characters`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!', '@', '#'))
            .build()

        val result = pipeline.execute("property!key@test#", config)

        assertEquals("propertykeytest", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Warning)
    }

    @Test
    fun `execute truncates to max length`() {
        val config = ValidationConfig.Builder()
            .addKeyLengthValidation(10)
            .build()

        val result = pipeline.execute("VeryLongPropertyKey", config)

        assertEquals("VeryLongPr", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Warning)
    }

    @Test
    fun `execute normalizes then validates`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!', '@'))
            .addKeyLengthValidation(15)
            .build()

        val result = pipeline.execute("property!key@test", config)

        // Should normalize first (remove !) then validate
        assertEquals("propertykeytest", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Warning)
    }

    @Test
    fun `execute returns Drop for empty cleaned key after normalization`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!', '@', '#'))
            .build()

        val result = pipeline.execute("!@#", config)

        assertEquals("", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Drop)
        assertEquals(DropReason.EMPTY_KEY, (result.outcome as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `execute handles comprehensive scenario with multiple modifications`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!', '@'))
            .addKeyLengthValidation(15)
            .build()

        val result = pipeline.execute("Very!Long@PropertyKeyName", config)

        assertEquals("VeryLongPropert", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Warning)
        assertEquals(2, result.outcome.errors.size)
    }

    @Test
    fun `execute reports all validation errors`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!'))
            .addKeyLengthValidation(10)
            .build()

        pipeline.execute("VeryLong!PropertyKey", config)

        verify { errorReporter.pushValidationResult(match<List<ValidationResult>> { it.size == 2 }) }
    }

    @Test
    fun `execute returns cleaned key even for dropped keys`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("", config)

        // Even though dropped, cleaned key is returned (empty in this case)
        assertEquals("", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Drop)
    }

    @Test
    fun `execute handles empty string input`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("", config)

        assertEquals("", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Drop)
        assertEquals(DropReason.EMPTY_KEY, (result.outcome as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `execute handles whitespace-only input`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("   ", config)

        assertEquals("", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Drop)
        assertEquals(DropReason.EMPTY_KEY, (result.outcome as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `execute with no config restrictions returns Success`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("ValidKey", config)

        assertEquals("ValidKey", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Success)
        assertTrue(result.outcome.errors.isEmpty())
    }

    @Test
    fun `execute handles unicode characters`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("属性键", config)

        assertEquals("属性键", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute removes unicode characters if disallowed`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('属', '性'))
            .build()

        val result = pipeline.execute("属性键", config)

        assertEquals("键", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Warning)
    }

    @Test
    fun `execute handles special characters`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('\n', '\t', '\r'))
            .build()

        val result = pipeline.execute("property\nkey\ttest\r", config)

        assertEquals("propertykeytest", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Warning)
    }

    @Test
    fun `execute with zero max length truncates to empty and drops`() {
        val config = ValidationConfig.Builder()
            .addKeyLengthValidation(0)
            .build()

        val result = pipeline.execute("PropertyKey", config)

        assertEquals("", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Drop)
        assertEquals(DropReason.EMPTY_KEY, (result.outcome as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `execute handles numbers in keys`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("property123", config)

        assertEquals("property123", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute handles mixed case keys`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("PropertyKey", config)

        assertEquals("PropertyKey", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute preserves spaces within key`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("property key test", config)

        assertEquals("property key test", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute handles dot notation in keys`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("user.profile.name", config)

        assertEquals("user.profile.name", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute handles underscore in keys`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("user_name", config)

        assertEquals("user_name", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute handles dash in keys`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("user-name", config)

        assertEquals("user-name", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute handles complex scenario with all validations`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!', '@', '#'))
            .addKeyLengthValidation(20)
            .build()

        // Test 1: Clean key
        val result1 = pipeline.execute("CleanPropertyKey", config)
        assertEquals("CleanPropertyKey", result1.cleanedKey)
        assertTrue(result1.outcome is ValidationOutcome.Success)

        // Test 2: Key with modifications
        val result2 = pipeline.execute("property!with@invalid#chars", config)
        assertEquals("propertywithinvalidc", result2.cleanedKey)
        assertTrue(result2.outcome is ValidationOutcome.Warning)

        // Test 3: Empty key
        val result3 = pipeline.execute("", config)
        assertEquals("", result3.cleanedKey)
        assertTrue(result3.outcome is ValidationOutcome.Drop)
    }

    @Test
    fun `execute outcome shouldDrop returns correct value`() {
        // Test Drop outcome
        val dropResult = pipeline.execute("", ValidationConfig.Builder().build())
        assertTrue(dropResult.shouldDrop())

        // Test Success outcome
        val successResult = pipeline.execute("validkey", ValidationConfig.Builder().build())
        assertFalse(successResult.shouldDrop())

        // Test Warning outcome
        val warningConfig = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!'))
            .build()
        val warningResult = pipeline.execute("key!", warningConfig)
        assertFalse(warningResult.shouldDrop())
    }

    @Test
    fun `execute applies normalization before validation`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!'))
            .addKeyLengthValidation(8)
            .build()

        val result = pipeline.execute("  test!key  ", config)

        // Should trim, remove !, then check length
        assertEquals("testkey", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Warning)
        // Should have warning for invalid character removal
        assertEquals(1, result.outcome.errors.size)
    }

    @Test
    fun `execute with null key pushes error to reporter`() {
        val config = ValidationConfig.Builder().build()

        pipeline.execute(null, config)

        verify(exactly = 1) { errorReporter.pushValidationResult(any<List<ValidationResult>>()) }
    }

    @Test
    fun `execute with valid key pushes empty errors to reporter`() {
        val config = ValidationConfig.Builder().build()

        pipeline.execute("validkey", config)

        verify { errorReporter.pushValidationResult(match<List<ValidationResult>> { it.isEmpty() }) }
    }

    @Test
    fun `execute with warnings pushes warnings to reporter`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!'))
            .build()

        pipeline.execute("key!", config)

        verify { errorReporter.pushValidationResult(match<List<ValidationResult>> { it.isNotEmpty() }) }
    }

    @Test
    fun `execute returns PropertyKeyValidationResult with correct structure`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("testkey", config)

        assertNotNull(result.cleanedKey)
        assertNotNull(result.outcome)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute handles camelCase keys`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("userProfileName", config)

        assertEquals("userProfileName", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute handles snake_case keys`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("user_profile_name", config)

        assertEquals("user_profile_name", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute handles kebab-case keys`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("user-profile-name", config)

        assertEquals("user-profile-name", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute handles keys with numbers at start`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("123key", config)

        assertEquals("123key", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute handles keys with numbers at end`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("key123", config)

        assertEquals("key123", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute handles single character keys`() {
        val config = ValidationConfig.Builder().build()

        val result = pipeline.execute("k", config)

        assertEquals("k", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Success)
    }

    @Test
    fun `execute handles very long keys with truncation`() {
        val config = ValidationConfig.Builder()
            .addKeyLengthValidation(50)
            .build()

        val longKey = "a".repeat(100)
        val result = pipeline.execute(longKey, config)

        assertEquals(50, result.cleanedKey.length)
        assertTrue(result.outcome is ValidationOutcome.Warning)
    }

    @Test
    fun `execute maintains character order after cleaning`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!', '@', '#'))
            .build()

        val result = pipeline.execute("a!b@c#d", config)

        assertEquals("abcd", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Warning)
    }

    @Test
    fun `execute handles consecutive disallowed characters`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!'))
            .build()

        val result = pipeline.execute("key!!!name", config)

        assertEquals("keyname", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Warning)
    }

    @Test
    fun `execute handles keys with only disallowed characters at boundaries`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!'))
            .build()

        val result = pipeline.execute("!!!key!!!", config)

        assertEquals("key", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Warning)
    }

    @Test
    fun `execute processes pipeline steps in correct order`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('X'))
            .addKeyLengthValidation(5)
            .build()

        // Input: "ABXCXDXEFX" -> after removing X: "ABCDEF" -> after truncate: "ABCDE"
        val result = pipeline.execute("ABXCXDXEFX", config)

        assertEquals("ABCDE", result.cleanedKey)
        assertTrue(result.outcome is ValidationOutcome.Warning)
    }
}
