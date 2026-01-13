package com.clevertap.android.sdk.validation.propertykey

import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.pipeline.ModificationReason
import com.clevertap.android.sdk.validation.pipeline.RemovalReason
import org.junit.Assert.*
import org.junit.Test

class EventPropertyKeyNormalizerTest {

    private val normalizer = EventPropertyKeyNormalizer()

    @Test
    fun `normalize returns empty for null input`() {
        val config = ValidationConfig.Builder().build()

        val result = normalizer.normalize(null, config)

        assertEquals("", result.originalKey)
        assertEquals("", result.cleanedKey)
        assertTrue(result.wasRemoved)
        assertEquals(RemovalReason.EMPTY_KEY, result.removalReason)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize trims whitespace from input`() {
        val config = ValidationConfig.Builder().build()

        val result = normalizer.normalize("  property key  ", config)

        assertEquals("  property key  ", result.originalKey)
        assertEquals("property key", result.cleanedKey)
        assertFalse(result.wasRemoved)
        assertNull(result.removalReason)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize with no restrictions returns same value`() {
        val config = ValidationConfig.Builder().build()

        val result = normalizer.normalize("ValidKey", config)

        assertEquals("ValidKey", result.originalKey)
        assertEquals("ValidKey", result.cleanedKey)
        assertFalse(result.wasRemoved)
        assertNull(result.removalReason)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize removes disallowed characters`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!', '@', '#'))
            .build()

        val result = normalizer.normalize("property!key@test#", config)

        assertEquals("property!key@test#", result.originalKey)
        assertEquals("propertykeytest", result.cleanedKey)
        assertFalse(result.wasRemoved)
        assertEquals(1, result.modifications.size)
        val modification = result.modifications.first()
        assertTrue(modification.reasons.contains(ModificationReason.INVALID_CHARACTERS_REMOVED))
    }

    @Test
    fun `normalize removes multiple disallowed characters`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('$', '%', '&', '*'))
            .build()

        val result = normalizer.normalize("\$property%key&test*", config)

        assertEquals("\$property%key&test*", result.originalKey)
        assertEquals("propertykeytest", result.cleanedKey)
        assertFalse(result.wasRemoved)
        assertEquals(1, result.modifications.size)
        val modification = result.modifications.first()
        assertTrue(modification.reasons.contains(ModificationReason.INVALID_CHARACTERS_REMOVED))
    }

    @Test
    fun `normalize does not modify when no disallowed chars present`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!', '@', '#'))
            .build()

        val result = normalizer.normalize("CleanPropertyKey", config)

        assertEquals("CleanPropertyKey", result.originalKey)
        assertEquals("CleanPropertyKey", result.cleanedKey)
        assertFalse(result.wasRemoved)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize truncates to max length`() {
        val config = ValidationConfig.Builder()
            .addKeyLengthValidation(10)
            .build()

        val result = normalizer.normalize("ThisIsAVeryLongPropertyKey", config)

        assertEquals("ThisIsAVeryLongPropertyKey", result.originalKey)
        assertEquals("ThisIsAVer", result.cleanedKey)
        assertFalse(result.wasRemoved)
        assertEquals(1, result.modifications.size)
        val modification = result.modifications.first()
        assertTrue(modification.reasons.contains(ModificationReason.TRUNCATED_TO_MAX_LENGTH))
    }

    @Test
    fun `normalize does not truncate when under max length`() {
        val config = ValidationConfig.Builder()
            .addKeyLengthValidation(50)
            .build()

        val result = normalizer.normalize("ShortKey", config)

        assertEquals("ShortKey", result.originalKey)
        assertEquals("ShortKey", result.cleanedKey)
        assertFalse(result.wasRemoved)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize does not truncate when exactly at max length`() {
        val config = ValidationConfig.Builder()
            .addKeyLengthValidation(10)
            .build()

        val result = normalizer.normalize("ExactlyTen", config)

        assertEquals("ExactlyTen", result.originalKey)
        assertEquals("ExactlyTen", result.cleanedKey)
        assertFalse(result.wasRemoved)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize applies both character removal and truncation`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!', '@'))
            .addKeyLengthValidation(10)
            .build()

        val result = normalizer.normalize("property!key@testLong", config)

        assertEquals("property!key@testLong", result.originalKey)
        assertEquals("propertyke", result.cleanedKey)
        assertFalse(result.wasRemoved)
        assertEquals(1, result.modifications.size)
        val modification = result.modifications.first()
        assertTrue(modification.reasons.contains(ModificationReason.INVALID_CHARACTERS_REMOVED))
        assertTrue(modification.reasons.contains(ModificationReason.TRUNCATED_TO_MAX_LENGTH))
    }

    @Test
    fun `normalize removes characters then truncates in correct order`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('X', 'Y', 'Z'))
            .addKeyLengthValidation(5)
            .build()

        val result = normalizer.normalize("ABXCYDZE", config)

        assertEquals("ABXCYDZE", result.originalKey)
        assertEquals("ABCDE", result.cleanedKey)
        assertFalse(result.wasRemoved)
        assertEquals(1, result.modifications.size)
        val modification = result.modifications.first()
        assertTrue(modification.reasons.contains(ModificationReason.INVALID_CHARACTERS_REMOVED))
        assertFalse(modification.reasons.contains(ModificationReason.TRUNCATED_TO_MAX_LENGTH))
    }

    @Test
    fun `normalize trims whitespace after all modifications`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!'))
            .addKeyLengthValidation(15)
            .build()

        val result = normalizer.normalize("  property!key  ", config)

        assertEquals("  property!key  ", result.originalKey)
        assertEquals("propertykey", result.cleanedKey)
        assertFalse(result.wasRemoved)
        assertEquals(1, result.modifications.size)
    }

    @Test
    fun `normalize handles empty string input`() {
        val config = ValidationConfig.Builder().build()

        val result = normalizer.normalize("", config)

        assertEquals("", result.originalKey)
        assertEquals("", result.cleanedKey)
        assertTrue(result.wasRemoved)
        assertEquals(RemovalReason.EMPTY_KEY, result.removalReason)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize handles whitespace-only input`() {
        val config = ValidationConfig.Builder().build()

        val result = normalizer.normalize("   ", config)

        assertEquals("   ", result.originalKey)
        assertEquals("", result.cleanedKey)
        assertTrue(result.wasRemoved)
        assertEquals(RemovalReason.EMPTY_KEY, result.removalReason)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize returns removed when all characters are disallowed`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!', '@', '#'))
            .build()

        val result = normalizer.normalize("!@#", config)

        assertEquals("!@#", result.originalKey)
        assertEquals("", result.cleanedKey)
        assertTrue(result.wasRemoved)
        assertEquals(RemovalReason.EMPTY_VALUE, result.removalReason)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize with null disallowed chars does not remove anything`() {
        val config = ValidationConfig.Builder().build()

        val result = normalizer.normalize("property!@#key", config)

        assertEquals("property!@#key", result.originalKey)
        assertEquals("property!@#key", result.cleanedKey)
        assertFalse(result.wasRemoved)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize with null max length does not truncate`() {
        val config = ValidationConfig.Builder().build()

        val result = normalizer.normalize("VeryLongPropertyKeyThatShouldNotBeTruncated", config)

        assertEquals("VeryLongPropertyKeyThatShouldNotBeTruncated", result.originalKey)
        assertEquals("VeryLongPropertyKeyThatShouldNotBeTruncated", result.cleanedKey)
        assertFalse(result.wasRemoved)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize with empty disallowed chars set does not remove anything`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(emptySet())
            .build()

        val result = normalizer.normalize("property!@#key", config)

        assertEquals("property!@#key", result.originalKey)
        assertEquals("property!@#key", result.cleanedKey)
        assertFalse(result.wasRemoved)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize handles unicode characters`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!'))
            .build()

        val result = normalizer.normalize("property你好!世界", config)

        assertEquals("property你好!世界", result.originalKey)
        assertEquals("property你好世界", result.cleanedKey)
        assertFalse(result.wasRemoved)
        assertEquals(1, result.modifications.size)
    }

    @Test
    fun `normalize handles special characters in disallowed set`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('\n', '\t', '\r'))
            .build()

        val result = normalizer.normalize("property\nkey\ttest\r", config)

        assertEquals("property\nkey\ttest\r", result.originalKey)
        assertEquals("propertykeytest", result.cleanedKey)
        assertFalse(result.wasRemoved)
        assertEquals(1, result.modifications.size)
    }

    @Test
    fun `normalize with zero max length truncates to empty`() {
        val config = ValidationConfig.Builder()
            .addKeyLengthValidation(0)
            .build()

        val result = normalizer.normalize("PropertyKey", config)

        assertEquals("PropertyKey", result.originalKey)
        assertEquals("", result.cleanedKey)
        assertTrue(result.wasRemoved)
        assertEquals(RemovalReason.EMPTY_VALUE, result.removalReason)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize preserves spaces within property key`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!'))
            .build()

        val result = normalizer.normalize("property key test", config)

        assertEquals("property key test", result.originalKey)
        assertEquals("property key test", result.cleanedKey)
        assertFalse(result.wasRemoved)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize handles mixed case characters`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!'))
            .build()

        val result = normalizer.normalize("PROPERTYkey!TEST", config)

        assertEquals("PROPERTYkey!TEST", result.originalKey)
        assertEquals("PROPERTYkeyTEST", result.cleanedKey)
        assertFalse(result.wasRemoved)
        assertEquals(1, result.modifications.size)
    }

    @Test
    fun `normalize handles numbers in property key`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!'))
            .addKeyLengthValidation(20)
            .build()

        val result = normalizer.normalize("property123!key456", config)

        assertEquals("property123!key456", result.originalKey)
        assertEquals("property123key456", result.cleanedKey)
        assertFalse(result.wasRemoved)
        assertEquals(1, result.modifications.size)
    }

    @Test
    fun `normalize marks removal with empty key reason for null`() {
        val config = ValidationConfig.Builder().build()

        val result = normalizer.normalize(null, config)

        assertTrue(result.wasRemoved)
        assertEquals(RemovalReason.EMPTY_KEY, result.removalReason)
    }

    @Test
    fun `normalize marks removal with empty value reason after cleaning`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('a', 'b', 'c'))
            .build()

        val result = normalizer.normalize("abc", config)

        assertTrue(result.wasRemoved)
        assertEquals(RemovalReason.EMPTY_VALUE, result.removalReason)
    }

    @Test
    fun `normalize records modification with both original and cleaned key`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!'))
            .build()

        val result = normalizer.normalize("test!key", config)

        assertEquals(1, result.modifications.size)
        val modification = result.modifications.first()
        assertEquals("test!key", modification.originalKey)
        assertEquals("testkey", modification.cleanedKey)
    }

    @Test
    fun `normalize does not record modification when key unchanged`() {
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!'))
            .addKeyLengthValidation(20)
            .build()

        val result = normalizer.normalize("cleankey", config)

        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize handles dot notation in keys`() {
        val config = ValidationConfig.Builder().build()

        val result = normalizer.normalize("user.profile.name", config)

        assertEquals("user.profile.name", result.originalKey)
        assertEquals("user.profile.name", result.cleanedKey)
        assertFalse(result.wasRemoved)
        assertTrue(result.modifications.isEmpty())
    }
}
