package com.clevertap.android.sdk.validation.eventdata

import com.clevertap.android.sdk.validation.ValidationConfig
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class EventDataNormalizerTest {

    private val normalizer = EventDataNormalizer()

    @Test
    fun `normalize returns empty JSONObject when input is null`() {
        val config = ValidationConfig.Builder().build()
        
        val result = normalizer.normalize(null, config)
        
        assertEquals(0, result.cleanedData.length())
    }

    @Test
    fun `normalize removes null values`() {
        val input = mapOf("key1" to "value1", "key2" to null)
        val config = ValidationConfig.Builder().build()
        
        val result = normalizer.normalize(input, config)
        
        assertEquals(1, result.cleanedData.length())
        assertEquals("value1", result.cleanedData.getString("key1"))
    }

    @Test
    fun `normalize removes empty keys`() {
        val input = mapOf("" to "value", "key" to "value2")
        val config = ValidationConfig.Builder().build()
        
        val result = normalizer.normalize(input, config)
        
        assertEquals(1, result.cleanedData.length())
        assertEquals("value2", result.cleanedData.getString("key"))
    }

    @Test
    fun `normalize trims keys and values`() {
        val input = mapOf("  key  " to "  value  ")
        val config = ValidationConfig.Builder().build()
        
        val result = normalizer.normalize(input, config)
        
        assertEquals("value", result.cleanedData.getString("key"))
    }

    @Test
    fun `normalize truncates key when exceeds max length`() {
        val longKey = "a".repeat(150)
        val input = mapOf(longKey to "value")
        val config = ValidationConfig.Builder()
            .addKeyLengthValidation(100)
            .build()

        val result = normalizer.normalize(input, config)

        val cleanedKey = result.cleanedData.keys().next() as String
        assertEquals(100, cleanedKey.length)
        assertTrue(result.metrics.keysModified.isNotEmpty())
    }

    @Test
    fun `normalize truncates value when exceeds max length`() {
        val longValue = "a".repeat(600)
        val input = mapOf("key" to longValue)
        val config = ValidationConfig.Builder()
            .addValueLengthValidation(500)
            .build()
        
        val result = normalizer.normalize(input, config)
        
        assertEquals(500, result.cleanedData.getString("key").length)
        assertTrue(result.metrics.valuesModified.isNotEmpty())
    }

    @Test
    fun `normalize removes disallowed chars from keys`() {
        val input = mapOf("key\$name" to "value")
        val config = ValidationConfig.Builder()
            .addKeyCharacterValidation(setOf('$'))
            .build()
        
        val result = normalizer.normalize(input, config)
        
        assertTrue(result.cleanedData.has("keyname"))
        assertTrue(result.metrics.keysModified.isNotEmpty())
    }

    @Test
    fun `normalize removes disallowed chars from values`() {
        val input = mapOf("key" to "val\$ue")
        val config = ValidationConfig.Builder()
            .addValueCharacterValidation(setOf('$'))
            .build()
        
        val result = normalizer.normalize(input, config)
        
        assertEquals("value", result.cleanedData.getString("key"))
        assertTrue(result.metrics.valuesModified.isNotEmpty())
    }

    @Test
    fun `normalize tracks max depth correctly`() {
        val input = mapOf(
            "level1" to mapOf(
                "level2" to mapOf(
                    "level3" to "value"
                )
            )
        )
        val config = ValidationConfig.Builder().build()
        
        val result = normalizer.normalize(input, config)
        
        assertEquals(2, result.metrics.maxDepth)
    }

    @Test
    fun `normalize handles arrays and tracks length`() {
        val input = mapOf("arr" to listOf(1, 2, 3, 4, 5))
        val config = ValidationConfig.Builder().build()
        
        val result = normalizer.normalize(input, config)
        
        assertEquals(5, result.metrics.maxArrayLength)
        val arr = result.cleanedData.getJSONArray("arr")
        assertEquals(5, arr.length())
    }

    @Test
    fun `normalize tracks array and object key counts`() {
        val input = mapOf(
            "arr1" to listOf(1, 2),
            "arr2" to listOf(3, 4),
            "obj1" to mapOf("a" to 1),
            "obj2" to mapOf("b" to 2),
            "primitive" to 42
        )
        val config = ValidationConfig.Builder().build()
        
        val result = normalizer.normalize(input, config)
        
        assertEquals(2, result.metrics.maxArrayKeyCount)
        assertEquals(2, result.metrics.maxObjectKeyCount)
    }

    @Test
    fun `normalize handles nested JSONObject`() {
        val nested = JSONObject().apply {
            put("inner", "value")
        }
        val input = mapOf("outer" to nested)
        val config = ValidationConfig.Builder().build()
        
        val result = normalizer.normalize(input, config)
        
        val outer = result.cleanedData.getJSONObject("outer")
        assertEquals("value", outer.getString("inner"))
    }

    @Test
    fun `normalize handles JSONArray`() {
        val arr = JSONArray().apply {
            put(1)
            put(2)
            put(3)
        }
        val input = mapOf("arr" to arr)
        val config = ValidationConfig.Builder().build()
        
        val result = normalizer.normalize(input, config)
        
        val cleaned = result.cleanedData.getJSONArray("arr")
        assertEquals(3, cleaned.length())
    }

    @Test
    fun `normalize removes restricted multi-value fields with nested values`() {
        val input = mapOf(
            "email" to mapOf("nested" to "value"),
            "identity" to listOf(1, 2, 3),
            "email2" to "simple@test.com"
        )
        val config = ValidationConfig.Builder()
            .setRestrictedMultiValueFields(setOf("email", "identity"))
            .build()
        
        val result = normalizer.normalize(input, config)
        
        assertEquals(1, result.cleanedData.length())
        assertEquals("simple@test.com", result.cleanedData.getString("email2"))
        assertEquals(2, result.metrics.itemsRemoved.size)
    }

    @Test
    fun `normalize handles phone validation when country code missing`() {
        val input = mapOf("Phone" to "1234567890")
        val config = ValidationConfig.Builder()
            .setDeviceCountryCodeProvider { null }
            .build()
        
        val result = normalizer.normalize(input, config)
        
        // Phone value should still be present but error recorded
        assertEquals("1234567890", result.cleanedData.getString("Phone"))
        assertTrue(result.metrics.itemsRemoved.any { it.key == "Phone" })
    }

    @Test
    fun `normalize handles phone validation with country code`() {
        val input = mapOf("phone" to "+911234567890")
        val config = ValidationConfig.Builder()
            .setDeviceCountryCodeProvider { "IN" }
            .build()
        
        val result = normalizer.normalize(input, config)
        
        assertEquals("+911234567890", result.cleanedData.getString("phone"))
    }

    @Test
    fun `normalize preserves primitive types`() {
        val input = mapOf(
            "int" to 42,
            "long" to 123L,
            "float" to 3.14f,
            "double" to 2.718,
            "bool" to true
        )
        val config = ValidationConfig.Builder().build()
        
        val result = normalizer.normalize(input, config)
        
        assertEquals(42, result.cleanedData.getInt("int"))
        assertEquals(123L, result.cleanedData.getLong("long"))
        assertEquals(3.14f, result.cleanedData.getDouble("float").toFloat(), 0.001f)
        assertEquals(2.718, result.cleanedData.getDouble("double"), 0.001)
        assertEquals(true, result.cleanedData.getBoolean("bool"))
    }

    @Test
    fun `normalize removes empty arrays after cleaning`() {
        val input = mapOf("arr" to listOf(null, null))
        val config = ValidationConfig.Builder().build()
        
        val result = normalizer.normalize(input, config)
        
        assertEquals(0, result.cleanedData.length())
    }

    @Test
    fun `normalize removes empty objects after cleaning`() {
        val input = mapOf("obj" to mapOf("key" to null))
        val config = ValidationConfig.Builder().build()
        
        val result = normalizer.normalize(input, config)
        
        assertEquals(0, result.cleanedData.length())
    }
}
