package com.clevertap.android.sdk.validation.propertykey

import com.clevertap.android.sdk.validation.DropReason
import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationError
import com.clevertap.android.sdk.validation.ValidationOutcome
import com.clevertap.android.sdk.validation.pipeline.ModificationReason
import com.clevertap.android.sdk.validation.pipeline.PropertyKeyNormalizationResult
import com.clevertap.android.sdk.validation.pipeline.RemovalReason
import org.junit.Assert.*
import org.junit.Test

class EventPropertyKeyValidatorTest {

    private val validator = EventPropertyKeyValidator()

    @Test
    fun `validate returns Drop for removed key with empty key reason`() {
        val input = PropertyKeyNormalizationResult(
            originalKey = "",
            cleanedKey = "",
            modifications = emptySet(),
            wasRemoved = true,
            removalReason = RemovalReason.EMPTY_KEY
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Drop)
        assertEquals(DropReason.EMPTY_KEY, (result as ValidationOutcome.Drop).reason)
        assertEquals(1, result.errors.size)
        assertEquals(ValidationError.EMPTY_KEY_ABORT.code, result.errors[0].errorCode)
    }

    @Test
    fun `validate returns Drop for removed key with empty value reason`() {
        val input = PropertyKeyNormalizationResult(
            originalKey = "!@#",
            cleanedKey = "",
            modifications = emptySet(),
            wasRemoved = true,
            removalReason = RemovalReason.EMPTY_VALUE
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Drop)
        assertEquals(DropReason.EMPTY_KEY, (result as ValidationOutcome.Drop).reason)
        assertEquals(1, result.errors.size)
        assertEquals(ValidationError.EMPTY_KEY_ABORT.code, result.errors[0].errorCode)
    }

    @Test
    fun `validate returns Success for valid key with no modifications`() {
        val input = PropertyKeyNormalizationResult(
            originalKey = "ValidKey",
            cleanedKey = "ValidKey",
            modifications = emptySet(),
            wasRemoved = false,
            removalReason = null
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate returns Warning for truncated key`() {
        val modification = com.clevertap.android.sdk.validation.pipeline.KeyModification(
            originalKey = "VeryLongPropertyKey",
            cleanedKey = "VeryLongPr",
            reasons = listOf(ModificationReason.TRUNCATED_TO_MAX_LENGTH)
        )
        val input = PropertyKeyNormalizationResult(
            originalKey = "VeryLongPropertyKey",
            cleanedKey = "VeryLongPr",
            modifications = setOf(modification),
            wasRemoved = false,
            removalReason = null
        )
        val config = ValidationConfig.Builder()
            .addKeyLengthValidation(10)
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Warning)
        assertEquals(1, result.errors.size)
        assertEquals(ValidationError.KEY_LENGTH_EXCEEDED.code, result.errors[0].errorCode)
    }

    @Test
    fun `validate returns Warning for invalid characters removed`() {
        val modification = com.clevertap.android.sdk.validation.pipeline.KeyModification(
            originalKey = "property!key",
            cleanedKey = "propertykey",
            reasons = listOf(ModificationReason.INVALID_CHARACTERS_REMOVED)
        )
        val input = PropertyKeyNormalizationResult(
            originalKey = "property!key",
            cleanedKey = "propertykey",
            modifications = setOf(modification),
            wasRemoved = false,
            removalReason = null
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Warning)
        assertEquals(1, result.errors.size)
        assertEquals(ValidationError.KEY_INVALID_CHARACTERS.code, result.errors[0].errorCode)
    }

    @Test
    fun `validate returns Warning for both truncation and invalid characters`() {
        val modification = com.clevertap.android.sdk.validation.pipeline.KeyModification(
            originalKey = "VeryLong!Property@Key",
            cleanedKey = "VeryLongPr",
            reasons = listOf(
                ModificationReason.TRUNCATED_TO_MAX_LENGTH,
                ModificationReason.INVALID_CHARACTERS_REMOVED
            )
        )
        val input = PropertyKeyNormalizationResult(
            originalKey = "VeryLong!Property@Key",
            cleanedKey = "VeryLongPr",
            modifications = setOf(modification),
            wasRemoved = false,
            removalReason = null
        )
        val config = ValidationConfig.Builder()
            .addKeyLengthValidation(10)
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Warning)
        assertEquals(2, result.errors.size)
        val errorCodes = result.errors.map { it.errorCode }.toSet()
        assertTrue(errorCodes.contains(ValidationError.KEY_INVALID_CHARACTERS.code))
        assertTrue(errorCodes.contains(ValidationError.KEY_LENGTH_EXCEEDED.code))
    }

    @Test
    fun `validate includes original and cleaned key in error message for invalid characters`() {
        val modification = com.clevertap.android.sdk.validation.pipeline.KeyModification(
            originalKey = "test!key",
            cleanedKey = "testkey",
            reasons = listOf(ModificationReason.INVALID_CHARACTERS_REMOVED)
        )
        val input = PropertyKeyNormalizationResult(
            originalKey = "test!key",
            cleanedKey = "testkey",
            modifications = setOf(modification),
            wasRemoved = false,
            removalReason = null
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Warning)
        val error = result.errors[0]
        assertEquals(ValidationError.KEY_INVALID_CHARACTERS.code, error.errorCode)
        assertTrue(error.errorDesc.contains("test!key"))
        assertTrue(error.errorDesc.contains("testkey"))
    }

    @Test
    fun `validate includes limit in error message for truncation`() {
        val modification = com.clevertap.android.sdk.validation.pipeline.KeyModification(
            originalKey = "VeryLongKey",
            cleanedKey = "VeryLo",
            reasons = listOf(ModificationReason.TRUNCATED_TO_MAX_LENGTH)
        )
        val input = PropertyKeyNormalizationResult(
            originalKey = "VeryLongKey",
            cleanedKey = "VeryLo",
            modifications = setOf(modification),
            wasRemoved = false,
            removalReason = null
        )
        val config = ValidationConfig.Builder()
            .addKeyLengthValidation(6)
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Warning)
        val error = result.errors[0]
        assertEquals(ValidationError.KEY_LENGTH_EXCEEDED.code, error.errorCode)
        assertTrue(error.errorDesc.contains("6"))
    }

    @Test
    fun `validate handles multiple modifications in single KeyModification`() {
        val modification = com.clevertap.android.sdk.validation.pipeline.KeyModification(
            originalKey = "original!key@value",
            cleanedKey = "originalk",
            reasons = listOf(
                ModificationReason.INVALID_CHARACTERS_REMOVED,
                ModificationReason.TRUNCATED_TO_MAX_LENGTH
            )
        )
        val input = PropertyKeyNormalizationResult(
            originalKey = "original!key@value",
            cleanedKey = "originalk",
            modifications = setOf(modification),
            wasRemoved = false,
            removalReason = null
        )
        val config = ValidationConfig.Builder()
            .addKeyLengthValidation(9)
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Warning)
        assertEquals(2, result.errors.size)
    }

    @Test
    fun `validate returns Success when no modifications present`() {
        val input = PropertyKeyNormalizationResult(
            originalKey = "normalkey",
            cleanedKey = "normalkey",
            modifications = emptySet(),
            wasRemoved = false,
            removalReason = null
        )
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('!'))
            .addKeyLengthValidation(20)
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate handles empty modifications set`() {
        val input = PropertyKeyNormalizationResult(
            originalKey = "key",
            cleanedKey = "key",
            modifications = emptySet(),
            wasRemoved = false,
            removalReason = null
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate skips truncation error when config maxKeyLength is null`() {
        val modification = com.clevertap.android.sdk.validation.pipeline.KeyModification(
            originalKey = "longkey",
            cleanedKey = "long",
            reasons = listOf(ModificationReason.TRUNCATED_TO_MAX_LENGTH)
        )
        val input = PropertyKeyNormalizationResult(
            originalKey = "longkey",
            cleanedKey = "long",
            modifications = setOf(modification),
            wasRemoved = false,
            removalReason = null
        )
        val config = ValidationConfig.Builder().build() // No max length set

        val result = validator.validate(input, config)

        // Should be Success because truncation error is skipped when maxKeyLength is null
        assertTrue(result is ValidationOutcome.Success || result is ValidationOutcome.Warning)
        if (result is ValidationOutcome.Warning) {
            // If Warning, it should not contain truncation error
            assertFalse(result.errors.any { it.errorCode == ValidationError.KEY_LENGTH_EXCEEDED.code })
        }
    }

    @Test
    fun `validate processes each modification reason separately`() {
        val modification1 = com.clevertap.android.sdk.validation.pipeline.KeyModification(
            originalKey = "key!1",
            cleanedKey = "key1",
            reasons = listOf(ModificationReason.INVALID_CHARACTERS_REMOVED)
        )
        val modification2 = com.clevertap.android.sdk.validation.pipeline.KeyModification(
            originalKey = "key1",
            cleanedKey = "key",
            reasons = listOf(ModificationReason.TRUNCATED_TO_MAX_LENGTH)
        )
        val input = PropertyKeyNormalizationResult(
            originalKey = "key!1",
            cleanedKey = "key",
            modifications = setOf(modification1, modification2),
            wasRemoved = false,
            removalReason = null
        )
        val config = ValidationConfig.Builder()
            .addKeyLengthValidation(3)
            .build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Warning)
        assertEquals(2, result.errors.size)
    }

    @Test
    fun `validate returns Drop outcome with empty key reason`() {
        val input = PropertyKeyNormalizationResult(
            originalKey = "",
            cleanedKey = "",
            modifications = emptySet(),
            wasRemoved = true,
            removalReason = RemovalReason.EMPTY_KEY
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Drop)
        assertEquals(DropReason.EMPTY_KEY, (result as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `validate handles unicode keys`() {
        val input = PropertyKeyNormalizationResult(
            originalKey = "属性键",
            cleanedKey = "属性键",
            modifications = emptySet(),
            wasRemoved = false,
            removalReason = null
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate handles numbers in keys`() {
        val input = PropertyKeyNormalizationResult(
            originalKey = "property123",
            cleanedKey = "property123",
            modifications = emptySet(),
            wasRemoved = false,
            removalReason = null
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate handles mixed case keys`() {
        val input = PropertyKeyNormalizationResult(
            originalKey = "PropertyKey",
            cleanedKey = "PropertyKey",
            modifications = emptySet(),
            wasRemoved = false,
            removalReason = null
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate handles dot notation in keys`() {
        val input = PropertyKeyNormalizationResult(
            originalKey = "user.profile.name",
            cleanedKey = "user.profile.name",
            modifications = emptySet(),
            wasRemoved = false,
            removalReason = null
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate handles underscore in keys`() {
        val input = PropertyKeyNormalizationResult(
            originalKey = "user_name",
            cleanedKey = "user_name",
            modifications = emptySet(),
            wasRemoved = false,
            removalReason = null
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate handles dash in keys`() {
        val input = PropertyKeyNormalizationResult(
            originalKey = "user-name",
            cleanedKey = "user-name",
            modifications = emptySet(),
            wasRemoved = false,
            removalReason = null
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate prioritizes Drop over Warning`() {
        val modification = com.clevertap.android.sdk.validation.pipeline.KeyModification(
            originalKey = "invalid!",
            cleanedKey = "",
            reasons = listOf(ModificationReason.INVALID_CHARACTERS_REMOVED)
        )
        val input = PropertyKeyNormalizationResult(
            originalKey = "invalid!",
            cleanedKey = "",
            modifications = setOf(modification),
            wasRemoved = true,
            removalReason = RemovalReason.EMPTY_VALUE
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        // Should be Drop, not Warning, even though there are modifications
        assertTrue(result is ValidationOutcome.Drop)
        assertEquals(DropReason.EMPTY_KEY, (result as ValidationOutcome.Drop).reason)
    }

    @Test
    fun `validate returns Warning outcome type correctly`() {
        val modification = com.clevertap.android.sdk.validation.pipeline.KeyModification(
            originalKey = "test!",
            cleanedKey = "test",
            reasons = listOf(ModificationReason.INVALID_CHARACTERS_REMOVED)
        )
        val input = PropertyKeyNormalizationResult(
            originalKey = "test!",
            cleanedKey = "test",
            modifications = setOf(modification),
            wasRemoved = false,
            removalReason = null
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Warning)
        assertFalse(result.errors.isEmpty())
    }

    @Test
    fun `validate returns Success outcome type correctly`() {
        val input = PropertyKeyNormalizationResult(
            originalKey = "validkey",
            cleanedKey = "validkey",
            modifications = emptySet(),
            wasRemoved = false,
            removalReason = null
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Success)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate creates proper ValidationResult for each error`() {
        val modification = com.clevertap.android.sdk.validation.pipeline.KeyModification(
            originalKey = "test!key",
            cleanedKey = "testkey",
            reasons = listOf(ModificationReason.INVALID_CHARACTERS_REMOVED)
        )
        val input = PropertyKeyNormalizationResult(
            originalKey = "test!key",
            cleanedKey = "testkey",
            modifications = setOf(modification),
            wasRemoved = false,
            removalReason = null
        )
        val config = ValidationConfig.Builder().build()

        val result = validator.validate(input, config)

        assertTrue(result is ValidationOutcome.Warning)
        val error = result.errors[0]
        assertNotNull(error.errorCode)
        assertNotNull(error.errorDesc)
        assertTrue(error.errorDesc.isNotEmpty())
    }
}
