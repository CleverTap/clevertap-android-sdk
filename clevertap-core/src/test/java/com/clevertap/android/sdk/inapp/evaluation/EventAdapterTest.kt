package com.clevertap.android.sdk.inapp.evaluation

import com.clevertap.android.sdk.Constants
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
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
        assertNull(result.numberValue())
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
        assertNull(result.value)
        assertNull(result.stringValue())
        assertNull(result.numberValue())
        assertNull(result.listValue())
        assertFalse(result.isList())
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
        result.onEach {
            assertNull(it.stringValue())
            assertNull(it.listValue())
        }
        assertEquals(listOf(10, 20), result.map { it.numberValue() })
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
        assertEquals(listOf<String?>(null, null), result.map { it.stringValue() })
    }

    @Test
    fun testGetItemValueEmptyList() {
        // Arrange
        val eventAdapter = EventAdapter("eventName", emptyMap())

        // Act
        val result = eventAdapter.getItemValue("itemPrice")

        // Assert
        assertEquals(listOf<String>(), result.map { it.stringValue() })
    }

    @Test
    fun `test isChargedEvent when event is charged returns true`() {
        // Arrange
        val eventAdapter = EventAdapter(Constants.CHARGED_EVENT, emptyMap())

        assertTrue(eventAdapter.isChargedEvent())
    }

    @Test
    fun `test isChargedEvent when event is not charged returns false`() {
        // Arrange
        val eventAdapter = EventAdapter("eventName", emptyMap())

        assertFalse(eventAdapter.isChargedEvent())
    }

    @Test
    fun `testGetProfileAttrName when missing`() {
        // Arrange
        val eventAdapter = EventAdapter("eventName", emptyMap())

        assertNull(eventAdapter.profileAttrName)
    }

    @Test
    fun `testGetProfileAttrName when present`() {
        // Arrange
        val eventAdapter = EventAdapter("eventName", emptyMap(), profileAttrName = "attr1")

        assertEquals("attr1", eventAdapter.profileAttrName)
    }
}