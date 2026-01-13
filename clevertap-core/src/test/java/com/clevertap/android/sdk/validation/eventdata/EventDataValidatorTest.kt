package com.clevertap.android.sdk.validation.eventdata

import com.clevertap.android.sdk.validation.ValidationConfig
import com.clevertap.android.sdk.validation.ValidationError
import com.clevertap.android.sdk.validation.ValidationOutcome
import com.clevertap.android.sdk.validation.pipeline.EventDataMetrics
import com.clevertap.android.sdk.validation.pipeline.EventDataNormalizationResult
import com.clevertap.android.sdk.validation.pipeline.KeyModification
import com.clevertap.android.sdk.validation.pipeline.ModificationReason
import com.clevertap.android.sdk.validation.pipeline.RemovalReason
import com.clevertap.android.sdk.validation.pipeline.RemovedItem
import com.clevertap.android.sdk.validation.pipeline.ValueModification
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class EventDataValidatorTest {

    private val validator = EventDataValidator()

    @Test
    fun `validate returns success when no limits exceeded`() {
        val input = EventDataNormalizationResult(
            cleanedData = JSONObject(),
            metrics = EventDataMetrics(
                maxDepth = 1,
                maxArrayKeyCount = 0,
                maxObjectKeyCount = 0,
                maxArrayLength = 0,
                maxKVPairCount = 1,
                keysModified = emptyList(),
                valuesModified = emptyList(),
                itemsRemoved = emptyList()
            )
        )
        val config = ValidationConfig.Builder().build()
        
        val outcome = validator.validate(input, config)
        
        assertTrue(outcome is ValidationOutcome.Success)
    }

    @Test
    fun `validate reports depth limit exceeded`() {
        val input = EventDataNormalizationResult(
            cleanedData = JSONObject(),
            metrics = EventDataMetrics(
                maxDepth = 15,
                maxArrayKeyCount = 0,
                maxObjectKeyCount = 0,
                maxArrayLength = 0,
                maxKVPairCount = 0,
                keysModified = emptyList(),
                valuesModified = emptyList(),
                itemsRemoved = emptyList()
            )
        )
        val config = ValidationConfig.Builder()
            .addDepthValidation(10)
            .build()
        
        val outcome = validator.validate(input, config)
        
        assertTrue(outcome is ValidationOutcome.Warning)
        assertTrue(outcome.errors.any { it.errorCode == ValidationError.DEPTH_LIMIT_EXCEEDED.code })
    }

    @Test
    fun `validate reports array key count limit exceeded`() {
        val input = EventDataNormalizationResult(
            cleanedData = JSONObject(),
            metrics = EventDataMetrics(
                maxDepth = 1,
                maxArrayKeyCount = 5,
                maxObjectKeyCount = 0,
                maxArrayLength = 0,
                maxKVPairCount = 0,
                keysModified = emptyList(),
                valuesModified = emptyList(),
                itemsRemoved = emptyList()
            )
        )
        val config = ValidationConfig.Builder()
            .addArrayKeyCountValidation(3)
            .build()
        
        val outcome = validator.validate(input, config)
        
        assertTrue(outcome is ValidationOutcome.Warning)
        assertTrue(outcome.errors.any { it.errorCode == ValidationError.ARRAY_KEY_COUNT_LIMIT_EXCEEDED.code })
    }

    @Test
    fun `validate reports object key count limit exceeded`() {
        val input = EventDataNormalizationResult(
            cleanedData = JSONObject(),
            metrics = EventDataMetrics(
                maxDepth = 1,
                maxArrayKeyCount = 0,
                maxObjectKeyCount = 8,
                maxArrayLength = 0,
                maxKVPairCount = 0,
                keysModified = emptyList(),
                valuesModified = emptyList(),
                itemsRemoved = emptyList()
            )
        )
        val config = ValidationConfig.Builder()
            .addObjectKeyCountValidation(5)
            .build()
        
        val outcome = validator.validate(input, config)
        
        assertTrue(outcome is ValidationOutcome.Warning)
        assertTrue(outcome.errors.any { it.errorCode == ValidationError.OBJECT_KEY_COUNT_LIMIT_EXCEEDED.code })
    }

    @Test
    fun `validate reports array length limit exceeded`() {
        val input = EventDataNormalizationResult(
            cleanedData = JSONObject(),
            metrics = EventDataMetrics(
                maxDepth = 1,
                maxArrayKeyCount = 0,
                maxObjectKeyCount = 0,
                maxArrayLength = 150,
                maxKVPairCount = 0,
                keysModified = emptyList(),
                valuesModified = emptyList(),
                itemsRemoved = emptyList()
            )
        )
        val config = ValidationConfig.Builder()
            .addArrayLengthValidation(100)
            .build()
        
        val outcome = validator.validate(input, config)
        
        assertTrue(outcome is ValidationOutcome.Warning)
        assertTrue(outcome.errors.any { it.errorCode == ValidationError.ARRAY_LENGTH_LIMIT_EXCEEDED.code })
    }

    @Test
    fun `validate reports kv pair count limit exceeded`() {
        val input = EventDataNormalizationResult(
            cleanedData = JSONObject(),
            metrics = EventDataMetrics(
                maxDepth = 1,
                maxArrayKeyCount = 0,
                maxObjectKeyCount = 0,
                maxArrayLength = 0,
                maxKVPairCount = 75,
                keysModified = emptyList(),
                valuesModified = emptyList(),
                itemsRemoved = emptyList()
            )
        )
        val config = ValidationConfig.Builder()
            .addKVPairCountValidation(50)
            .build()
        
        val outcome = validator.validate(input, config)
        
        assertTrue(outcome is ValidationOutcome.Warning)
        assertTrue(outcome.errors.any { it.errorCode == ValidationError.KV_PAIR_COUNT_LIMIT_EXCEEDED.code })
    }

    @Test
    fun `validate reports key modifications for invalid characters`() {
        val input = EventDataNormalizationResult(
            cleanedData = JSONObject(),
            metrics = EventDataMetrics(
                maxDepth = 1,
                maxArrayKeyCount = 0,
                maxObjectKeyCount = 0,
                maxArrayLength = 0,
                maxKVPairCount = 0,
                keysModified = listOf(
                    KeyModification(
                        originalKey = "key\$name",
                        cleanedKey = "keyname",
                        reasons = listOf(ModificationReason.INVALID_CHARACTERS_REMOVED)
                    )
                ),
                valuesModified = emptyList(),
                itemsRemoved = emptyList()
            )
        )
        val config = ValidationConfig.Builder().build()
        
        val outcome = validator.validate(input, config)
        
        assertTrue(outcome is ValidationOutcome.Warning)
        assertTrue(outcome.errors.any { it.errorCode == ValidationError.KEY_INVALID_CHARACTERS.code })
    }

    @Test
    fun `validate reports key modifications for truncation`() {
        val input = EventDataNormalizationResult(
            cleanedData = JSONObject(),
            metrics = EventDataMetrics(
                maxDepth = 1,
                maxArrayKeyCount = 0,
                maxObjectKeyCount = 0,
                maxArrayLength = 0,
                maxKVPairCount = 0,
                keysModified = listOf(
                    KeyModification(
                        originalKey = "a".repeat(150),
                        cleanedKey = "a".repeat(100),
                        reasons = listOf(ModificationReason.TRUNCATED_TO_MAX_LENGTH)
                    )
                ),
                valuesModified = emptyList(),
                itemsRemoved = emptyList()
            )
        )
        val config = ValidationConfig.Builder()
            .addKeyLengthValidation(100)
            .build()
        
        val outcome = validator.validate(input, config)
        
        assertTrue(outcome is ValidationOutcome.Warning)
        assertTrue(outcome.errors.any { it.errorCode == ValidationError.KEY_LENGTH_EXCEEDED.code })
    }

    @Test
    fun `validate reports value modifications for invalid characters`() {
        val input = EventDataNormalizationResult(
            cleanedData = JSONObject(),
            metrics = EventDataMetrics(
                maxDepth = 1,
                maxArrayKeyCount = 0,
                maxObjectKeyCount = 0,
                maxArrayLength = 0,
                maxKVPairCount = 0,
                keysModified = emptyList(),
                valuesModified = listOf(
                    ValueModification(
                        key = "key",
                        originalValue = "val\$ue",
                        cleanedValue = "value",
                        reasons = listOf(ModificationReason.INVALID_CHARACTERS_REMOVED)
                    )
                ),
                itemsRemoved = emptyList()
            )
        )
        val config = ValidationConfig.Builder().build()
        
        val outcome = validator.validate(input, config)
        
        assertTrue(outcome is ValidationOutcome.Warning)
        assertTrue(outcome.errors.any { it.errorCode == ValidationError.VALUE_INVALID_CHARACTERS.code })
    }

    @Test
    fun `validate reports value modifications for truncation`() {
        val input = EventDataNormalizationResult(
            cleanedData = JSONObject(),
            metrics = EventDataMetrics(
                maxDepth = 1,
                maxArrayKeyCount = 0,
                maxObjectKeyCount = 0,
                maxArrayLength = 0,
                maxKVPairCount = 0,
                keysModified = emptyList(),
                valuesModified = listOf(
                    ValueModification(
                        key = "key",
                        originalValue = "a".repeat(600),
                        cleanedValue = "a".repeat(500),
                        reasons = listOf(ModificationReason.TRUNCATED_TO_MAX_LENGTH)
                    )
                ),
                itemsRemoved = emptyList()
            )
        )
        val config = ValidationConfig.Builder()
            .addValueLengthValidation(500)
            .build()
        
        val outcome = validator.validate(input, config)
        
        assertTrue(outcome is ValidationOutcome.Warning)
        assertTrue(outcome.errors.any { it.errorCode == ValidationError.VALUE_CHARS_LIMIT_EXCEEDED.code })
    }

    @Test
    fun `validate reports removed items for null values`() {
        val input = EventDataNormalizationResult(
            cleanedData = JSONObject(),
            metrics = EventDataMetrics(
                maxDepth = 1,
                maxArrayKeyCount = 0,
                maxObjectKeyCount = 0,
                maxArrayLength = 0,
                maxKVPairCount = 0,
                keysModified = emptyList(),
                valuesModified = emptyList(),
                itemsRemoved = listOf(
                    RemovedItem("key", RemovalReason.NULL_VALUE, null)
                )
            )
        )
        val config = ValidationConfig.Builder().build()
        
        val outcome = validator.validate(input, config)
        
        assertTrue(outcome is ValidationOutcome.Warning)
        assertTrue(outcome.errors.any { it.errorCode == ValidationError.NULL_VALUE_REMOVED.code })
    }

    @Test
    fun `validate reports removed items for empty values`() {
        val input = EventDataNormalizationResult(
            cleanedData = JSONObject(),
            metrics = EventDataMetrics(
                maxDepth = 1,
                maxArrayKeyCount = 0,
                maxObjectKeyCount = 0,
                maxArrayLength = 0,
                maxKVPairCount = 0,
                keysModified = emptyList(),
                valuesModified = emptyList(),
                itemsRemoved = listOf(
                    RemovedItem("key", RemovalReason.EMPTY_VALUE, "")
                )
            )
        )
        val config = ValidationConfig.Builder().build()
        
        val outcome = validator.validate(input, config)
        
        assertTrue(outcome is ValidationOutcome.Warning)
        assertTrue(outcome.errors.any { it.errorCode == ValidationError.EMPTY_VALUE_REMOVED.code })
    }

    @Test
    fun `validate reports removed items for empty keys`() {
        val input = EventDataNormalizationResult(
            cleanedData = JSONObject(),
            metrics = EventDataMetrics(
                maxDepth = 1,
                maxArrayKeyCount = 0,
                maxObjectKeyCount = 0,
                maxArrayLength = 0,
                maxKVPairCount = 0,
                keysModified = emptyList(),
                valuesModified = emptyList(),
                itemsRemoved = listOf(
                    RemovedItem("", RemovalReason.EMPTY_KEY, "value")
                )
            )
        )
        val config = ValidationConfig.Builder().build()
        
        val outcome = validator.validate(input, config)
        
        assertTrue(outcome is ValidationOutcome.Warning)
        assertTrue(outcome.errors.any { it.errorCode == ValidationError.EMPTY_KEY.code })
    }

    @Test
    fun `validate reports removed items for non-primitive values`() {
        val input = EventDataNormalizationResult(
            cleanedData = JSONObject(),
            metrics = EventDataMetrics(
                maxDepth = 1,
                maxArrayKeyCount = 0,
                maxObjectKeyCount = 0,
                maxArrayLength = 0,
                maxKVPairCount = 0,
                keysModified = emptyList(),
                valuesModified = emptyList(),
                itemsRemoved = listOf(
                    RemovedItem("key", RemovalReason.NON_PRIMITIVE_VALUE, Any())
                )
            )
        )
        val config = ValidationConfig.Builder().build()
        
        val outcome = validator.validate(input, config)
        
        assertTrue(outcome is ValidationOutcome.Warning)
        assertTrue(outcome.errors.any { it.errorCode == ValidationError.PROP_VALUE_NOT_PRIMITIVE.code })
    }

    @Test
    fun `validate reports removed items for invalid phone`() {
        val input = EventDataNormalizationResult(
            cleanedData = JSONObject(),
            metrics = EventDataMetrics(
                maxDepth = 1,
                maxArrayKeyCount = 0,
                maxObjectKeyCount = 0,
                maxArrayLength = 0,
                maxKVPairCount = 0,
                keysModified = emptyList(),
                valuesModified = emptyList(),
                itemsRemoved = listOf(
                    RemovedItem("Phone", RemovalReason.INVALID_PHONE_NUMBER, "12345")
                )
            )
        )
        val config = ValidationConfig.Builder().build()
        
        val outcome = validator.validate(input, config)
        
        assertTrue(outcome is ValidationOutcome.Warning)
        assertTrue(outcome.errors.any { it.errorCode == ValidationError.INVALID_PHONE.code })
    }

    @Test
    fun `validate reports removed items for invalid country code`() {
        val input = EventDataNormalizationResult(
            cleanedData = JSONObject(),
            metrics = EventDataMetrics(
                maxDepth = 1,
                maxArrayKeyCount = 0,
                maxObjectKeyCount = 0,
                maxArrayLength = 0,
                maxKVPairCount = 0,
                keysModified = emptyList(),
                valuesModified = emptyList(),
                itemsRemoved = listOf(
                    RemovedItem("Phone", RemovalReason.INVALID_COUNTRY_CODE, "1234567890")
                )
            )
        )
        val config = ValidationConfig.Builder().build()
        
        val outcome = validator.validate(input, config)
        
        assertTrue(outcome is ValidationOutcome.Warning)
        assertTrue(outcome.errors.any { it.errorCode == ValidationError.INVALID_COUNTRY_CODE.code })
    }

    @Test
    fun `validate reports removed items for restricted multi-value keys`() {
        val input = EventDataNormalizationResult(
            cleanedData = JSONObject(),
            metrics = EventDataMetrics(
                maxDepth = 1,
                maxArrayKeyCount = 0,
                maxObjectKeyCount = 0,
                maxArrayLength = 0,
                maxKVPairCount = 0,
                keysModified = emptyList(),
                valuesModified = emptyList(),
                itemsRemoved = listOf(
                    RemovedItem("email", RemovalReason.RESTRICTED_KEY_NESTED_VALUE, mapOf("a" to 1))
                )
            )
        )
        val config = ValidationConfig.Builder().build()
        
        val outcome = validator.validate(input, config)
        
        assertTrue(outcome is ValidationOutcome.Warning)
        assertTrue(outcome.errors.any { it.errorCode == ValidationError.RESTRICTED_MULTI_VALUE_KEY.code })
    }

    @Test
    fun `validate aggregates multiple errors`() {
        val input = EventDataNormalizationResult(
            cleanedData = JSONObject(),
            metrics = EventDataMetrics(
                maxDepth = 15,
                maxArrayKeyCount = 0,
                maxObjectKeyCount = 0,
                maxArrayLength = 0,
                maxKVPairCount = 75,
                keysModified = listOf(
                    KeyModification(
                        originalKey = "key\$",
                        cleanedKey = "key",
                        reasons = listOf(ModificationReason.INVALID_CHARACTERS_REMOVED)
                    )
                ),
                valuesModified = emptyList(),
                itemsRemoved = listOf(
                    RemovedItem("key", RemovalReason.NULL_VALUE, null)
                )
            )
        )
        val config = ValidationConfig.Builder()
            .addDepthValidation(10)
            .addKVPairCountValidation(50)
            .build()
        
        val outcome = validator.validate(input, config)
        
        assertTrue(outcome is ValidationOutcome.Warning)
        assertEquals(4, outcome.errors.size)
    }
}
