package com.clevertap.android.sdk.inapp.matchers

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventAdapterTest : BaseTestCase() {
    @Test
    fun testGetPropertyPresent() {
        // Arrange
        val eventProperties = mapOf("name" to "John", "age" to 30)
        val eventAdapter = EventAdapter("eventName", eventProperties)

        // Act
        val result = eventAdapter.getPropertyValue("name")
        val expected = TriggerValue("John")

        // Assert
        assertNotNull(result)
        assertEquals(expected.stringValue(), result.stringValue())
        assertEquals(.0, result.numberValue())
        assertNull(result.listValue())
        assertFalse { expected.isList() }
    }

    @Test
    fun testGetPropertyMissing() {
        // Arrange
        val eventProperties = mapOf("age" to 30)
        val eventAdapter = EventAdapter("eventName", eventProperties)

        // Act
        val result = eventAdapter.getPropertyValue("name")

        // Assert
        assertNull(result)
    }

    @Test
    fun testGetItemValuePresent() {
        // Arrange
        val items = listOf(
            mapOf("itemName" to "item1", "itemPrice" to 10),
            mapOf("itemName" to "item2", "itemPrice" to 20)
        )
        val eventAdapter = EventAdapter("eventName", emptyMap(), items)
        val expected = TriggerValue(listOf(10, 20))

        // Act
        val result = eventAdapter.getItemValue("itemPrice")

        // Assert
        assertNotNull(result)
        assertEquals("", result.stringValue())
        assertEquals(.0, result.numberValue())
        assertEquals(listOf(10, 20), result.listValue())
        assertTrue(expected.isList())

    }

    @Test
    fun testGetItemValueMissing() {
        // Arrange
        val items = listOf(
            mapOf("itemName" to "item1"),
            mapOf("itemName" to "item2")
        )
        val eventAdapter = EventAdapter("eventName", emptyMap(), items)

        // Act
        val result = eventAdapter.getItemValue("itemPrice")

        // Assert
        assertNull(result)
    }

    @Test
    fun testGetItemValueEmptyList() {
        // Arrange
        val eventAdapter = EventAdapter("eventName", emptyMap())

        // Act
        val result = eventAdapter.getItemValue("itemPrice")

        // Assert
        assertNull(result)
    }
}