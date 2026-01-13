package com.clevertap.android.sdk.validation.chargedevent

import com.clevertap.android.sdk.validation.ValidationConfig
import org.junit.Assert.*
import org.junit.Test

class ChargedEventItemsNormalizerTest {

    private val normalizer = ChargedEventItemsNormalizer()

    @Test
    fun `normalize returns zero count for null input`() {
        val config = ValidationConfig.Builder().build()

        val result = normalizer.normalize(null, config)

        assertEquals(0, result.itemsCount)
    }

    @Test
    fun `normalize returns zero count for empty list`() {
        val config = ValidationConfig.Builder().build()

        val result = normalizer.normalize(emptyList<Any>(), config)

        assertEquals(0, result.itemsCount)
    }

    @Test
    fun `normalize counts many items`() {
        val config = ValidationConfig.Builder().build()
        val items = (1..100).map { mapOf("id" to it) }

        val result = normalizer.normalize(items, config)

        assertEquals(100, result.itemsCount)
    }

    @Test
    fun `normalize handles list with null items`() {
        val config = ValidationConfig.Builder().build()
        val items = listOf(
            mapOf("name" to "Item1"),
            null,
            mapOf("name" to "Item2")
        )

        val result = normalizer.normalize(items, config)

        assertEquals(3, result.itemsCount)
    }

    @Test
    fun `normalize handles list with maps of different sizes`() {
        val config = ValidationConfig.Builder().build()
        val items = listOf(
            mapOf("name" to "Item1"),
            mapOf("name" to "Item2", "price" to 100),
            mapOf("name" to "Item3", "price" to 200, "category" to "Electronics")
        )

        val result = normalizer.normalize(items, config)

        assertEquals(3, result.itemsCount)
    }

    @Test
    fun `normalize handles list with nested structures`() {
        val config = ValidationConfig.Builder().build()
        val items = listOf(
            mapOf("name" to "Item1", "details" to mapOf("color" to "red")),
            mapOf("name" to "Item2")
        )

        val result = normalizer.normalize(items, config)

        assertEquals(2, result.itemsCount)
    }
}
