package com.clevertap.android.sdk.validation.eventname

import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.pipeline.ModificationReason
import org.junit.Assert.*
import org.junit.Test

class EventNameNormalizerTest {

    private val normalizer = EventNameNormalizer()

    @Test
    fun `normalize returns empty string for null input`() {
        val config = ValidationConfig.Builder().build()

        val result = normalizer.normalize(null, config)

        assertNull(result.originalName)
        assertEquals("", result.cleanedName)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize trims whitespace from input`() {
        val config = ValidationConfig.Builder().build()

        val result = normalizer.normalize("  event name  ", config)

        assertEquals("  event name  ", result.originalName)
        assertEquals("event name", result.cleanedName)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize with no restrictions returns same value`() {
        val config = ValidationConfig.Builder().build()

        val result = normalizer.normalize("ValidEvent", config)

        assertEquals("ValidEvent", result.originalName)
        assertEquals("ValidEvent", result.cleanedName)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize removes disallowed characters`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('!', '@', '#'))
            .build()

        val result = normalizer.normalize("Event!Name@Test#", config)

        assertEquals("Event!Name@Test#", result.originalName)
        assertEquals("EventNameTest", result.cleanedName)
        assertTrue(result.modifications.contains(ModificationReason.INVALID_CHARACTERS_REMOVED))
    }

    @Test
    fun `normalize removes multiple disallowed characters`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('$', '%', '&', '*'))
            .build()

        val result = normalizer.normalize("\$Event%Name&Test*", config)

        assertEquals("\$Event%Name&Test*", result.originalName)
        assertEquals("EventNameTest", result.cleanedName)
        assertTrue(result.modifications.contains(ModificationReason.INVALID_CHARACTERS_REMOVED))
    }

    @Test
    fun `normalize does not modify when no disallowed chars present`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('!', '@', '#'))
            .build()

        val result = normalizer.normalize("CleanEventName", config)

        assertEquals("CleanEventName", result.originalName)
        assertEquals("CleanEventName", result.cleanedName)
        assertFalse(result.modifications.contains(ModificationReason.INVALID_CHARACTERS_REMOVED))
    }

    @Test
    fun `normalize truncates to max length`() {
        val config = ValidationConfig.Builder()
            .addEventNameLengthValidation(10)
            .build()

        val result = normalizer.normalize("ThisIsAVeryLongEventName", config)

        assertEquals("ThisIsAVeryLongEventName", result.originalName)
        assertEquals("ThisIsAVer", result.cleanedName)
        assertTrue(result.modifications.contains(ModificationReason.TRUNCATED_TO_MAX_LENGTH))
    }

    @Test
    fun `normalize does not truncate when under max length`() {
        val config = ValidationConfig.Builder()
            .addEventNameLengthValidation(50)
            .build()

        val result = normalizer.normalize("ShortName", config)

        assertEquals("ShortName", result.originalName)
        assertEquals("ShortName", result.cleanedName)
        assertFalse(result.modifications.contains(ModificationReason.TRUNCATED_TO_MAX_LENGTH))
    }

    @Test
    fun `normalize does not truncate when exactly at max length`() {
        val config = ValidationConfig.Builder()
            .addEventNameLengthValidation(10)
            .build()

        val result = normalizer.normalize("ExactlyTen", config)

        assertEquals("ExactlyTen", result.originalName)
        assertEquals("ExactlyTen", result.cleanedName)
        assertFalse(result.modifications.contains(ModificationReason.TRUNCATED_TO_MAX_LENGTH))
    }

    @Test
    fun `normalize applies both character removal and truncation`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('!', '@'))
            .addEventNameLengthValidation(10)
            .build()

        val result = normalizer.normalize("Event!Name@TestLong", config)

        assertEquals("Event!Name@TestLong", result.originalName)
        assertEquals("EventNameT", result.cleanedName)
        assertTrue(result.modifications.contains(ModificationReason.INVALID_CHARACTERS_REMOVED))
        assertTrue(result.modifications.contains(ModificationReason.TRUNCATED_TO_MAX_LENGTH))
    }

    @Test
    fun `normalize removes characters then truncates in correct order`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('X', 'Y', 'Z'))
            .addEventNameLengthValidation(5)
            .build()

        val result = normalizer.normalize("ABXCYDZE", config)

        assertEquals("ABXCYDZE", result.originalName)
        assertEquals("ABCDE", result.cleanedName)
        assertTrue(result.modifications.contains(ModificationReason.INVALID_CHARACTERS_REMOVED))
        assertFalse(result.modifications.contains(ModificationReason.TRUNCATED_TO_MAX_LENGTH))
    }

    @Test
    fun `normalize trims whitespace after all modifications`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('!'))
            .addEventNameLengthValidation(15)
            .build()

        val result = normalizer.normalize("  Event!Name  ", config)

        assertEquals("  Event!Name  ", result.originalName)
        assertEquals("EventName", result.cleanedName)
        assertTrue(result.modifications.contains(ModificationReason.INVALID_CHARACTERS_REMOVED))
    }

    @Test
    fun `normalize handles empty string input`() {
        val config = ValidationConfig.Builder().build()

        val result = normalizer.normalize("", config)

        assertEquals("", result.originalName)
        assertEquals("", result.cleanedName)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize handles whitespace-only input`() {
        val config = ValidationConfig.Builder().build()

        val result = normalizer.normalize("   ", config)

        assertEquals("   ", result.originalName)
        assertEquals("", result.cleanedName)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize removes all characters when all are disallowed`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('!', '@', '#'))
            .build()

        val result = normalizer.normalize("!@#", config)

        assertEquals("!@#", result.originalName)
        assertEquals("", result.cleanedName)
        assertTrue(result.modifications.contains(ModificationReason.INVALID_CHARACTERS_REMOVED))
    }

    @Test
    fun `normalize with null disallowed chars does not remove anything`() {
        val config = ValidationConfig.Builder().build()

        val result = normalizer.normalize("Event!@#Name", config)

        assertEquals("Event!@#Name", result.originalName)
        assertEquals("Event!@#Name", result.cleanedName)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize with null max length does not truncate`() {
        val config = ValidationConfig.Builder().build()

        val result = normalizer.normalize("VeryLongEventNameThatShouldNotBeTruncated", config)

        assertEquals("VeryLongEventNameThatShouldNotBeTruncated", result.originalName)
        assertEquals("VeryLongEventNameThatShouldNotBeTruncated", result.cleanedName)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize with empty disallowed chars set does not remove anything`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(emptySet())
            .build()

        val result = normalizer.normalize("Event!@#Name", config)

        assertEquals("Event!@#Name", result.originalName)
        assertEquals("Event!@#Name", result.cleanedName)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize handles unicode characters`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('!'))
            .build()

        val result = normalizer.normalize("Event你好!世界", config)

        assertEquals("Event你好!世界", result.originalName)
        assertEquals("Event你好世界", result.cleanedName)
        assertTrue(result.modifications.contains(ModificationReason.INVALID_CHARACTERS_REMOVED))
    }

    @Test
    fun `normalize handles special characters in disallowed set`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('\n', '\t', '\r'))
            .build()

        val result = normalizer.normalize("Event\nName\tTest\r", config)

        assertEquals("Event\nName\tTest\r", result.originalName)
        assertEquals("EventNameTest", result.cleanedName)
        assertTrue(result.modifications.contains(ModificationReason.INVALID_CHARACTERS_REMOVED))
    }

    @Test
    fun `normalize with zero max length truncates to empty`() {
        val config = ValidationConfig.Builder()
            .addEventNameLengthValidation(0)
            .build()

        val result = normalizer.normalize("EventName", config)

        assertEquals("EventName", result.originalName)
        assertEquals("", result.cleanedName)
        assertTrue(result.modifications.contains(ModificationReason.TRUNCATED_TO_MAX_LENGTH))
    }

    @Test
    fun `normalize preserves spaces within event name`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('!'))
            .build()

        val result = normalizer.normalize("Event Name Test", config)

        assertEquals("Event Name Test", result.originalName)
        assertEquals("Event Name Test", result.cleanedName)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize handles mixed case characters`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('!'))
            .build()

        val result = normalizer.normalize("EVENTname!TEST", config)

        assertEquals("EVENTname!TEST", result.originalName)
        assertEquals("EVENTnameTEST", result.cleanedName)
        assertTrue(result.modifications.contains(ModificationReason.INVALID_CHARACTERS_REMOVED))
    }

    @Test
    fun `normalize handles numbers in event name`() {
        val config = ValidationConfig.Builder()
            .addEventNameCharacterValidation(setOf('!'))
            .addEventNameLengthValidation(20)
            .build()

        val result = normalizer.normalize("Event123!Name456", config)

        assertEquals("Event123!Name456", result.originalName)
        assertEquals("Event123Name456", result.cleanedName)
        assertTrue(result.modifications.contains(ModificationReason.INVALID_CHARACTERS_REMOVED))
    }
}
