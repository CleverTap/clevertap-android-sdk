package com.clevertap.android.sdk.inapp.evaluation

import com.clevertap.android.sdk.Constants
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.Assert.assertNotEquals
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
    fun testGetPropertyPresentSpecialCases() {
        // Arrange
        val eventProperties = mapOf(
            "name" to "John",
            Constants.CLTAP_PROP_CAMPAIGN_ID to "cpid",
            Constants.NOTIFICATION_ID_TAG to "some_wzrk_id",
            Constants.CLTAP_PROP_VARIANT to "some variant",
            Constants.INAPP_WZRK_PIVOT to "some_wzrk_pivot",
        )
        val eventAdapter = EventAdapter("eventName", eventProperties)

        val triggerProps = listOf(
            Constants.CLTAP_PROP_CAMPAIGN_ID,
            Constants.NOTIFICATION_ID_TAG,
            Constants.CLTAP_PROP_VARIANT,
            Constants.INAPP_WZRK_PIVOT
        )

        val expectedResults = listOf(
            TriggerValue("cpid"),
            TriggerValue("some_wzrk_id"),
            TriggerValue("some variant"),
            TriggerValue("some_wzrk_pivot")
        )

        val results = mutableListOf<TriggerValue>().apply {
            triggerProps.forEach {
                add(eventAdapter.getPropertyValue(it))
            }
        }

        results.forEachIndexed { index, result ->
            val expected = expectedResults[index]
            // Assert
            assertNotNull(result)
            assertEquals(expected.stringValue(), result.stringValue())
            assertNull(result.numberValue())
            assertNull(result.listValue())
            assertFalse { expected.isList() }
        }
    }

    @Test
    fun testGetPropertyPresentNormalisations() {
        // Arrange
        val eventProperties = mapOf("   name " to "John", "a ge  " to 30)
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

        // Act
        val result1 = eventAdapter.getPropertyValue("age")
        val expected1 = TriggerValue(30)

        // Assert
        assertNotNull(result1)
        assertEquals(expected1.numberValue(), result1.numberValue())
        assertNull(result1.stringValue())
        assertNull(result1.listValue())
        assertFalse { expected1.isList() }
    }

    @Test
    fun testGetPropertyPresentNormalisations2() {
        // Arrange
        val eventProperties = mapOf("   name " to "John Doe", "Lucky Numbers" to listOf(1, 2))
        val eventAdapter = EventAdapter("eventName", eventProperties)

        // Act
        val result = eventAdapter.getPropertyValue("name")
        val expected = TriggerValue("John Doe")
        val notExpected = TriggerValue("JohnDoe")

        // Assert
        assertNotNull(result)
        assertEquals(expected.stringValue(), result.stringValue())
        assertNotEquals(notExpected.stringValue(), result.stringValue())
        assertNull(result.numberValue())
        assertNull(result.listValue())
        assertFalse { expected.isList() }

        // Act
        val result1 = eventAdapter.getPropertyValue("luckynumbers")
        val expected1 = TriggerValue(
            value = null,
            listValue = listOf(1, 2)
        )

        // Assert
        assertNotNull(result1)
        assertEquals(expected1.listValue(), result1.listValue())
        assertNull(result1.stringValue())
        assertNull(result1.numberValue())
        assertTrue { expected1.isList() }
    }

    @Test
    fun testGetPropertyPresentNormalisationsExactMatch() {
        // Arrange
        val eventProperties = mapOf(
            "fullname" to "John",
            "full name" to "Lenon",
            "FullName" to "Augustus",
            "age" to 30
        )
        val eventAdapter = EventAdapter("eventName", eventProperties)

        // Act
        val result = eventAdapter.getPropertyValue("fullname")
        val expected = TriggerValue("John")

        // Assert
        assertNotNull(result)
        assertEquals(expected.stringValue(), result.stringValue())
        assertNull(result.numberValue())
        assertNull(result.listValue())
        assertFalse { expected.isList() }

        // Act
        val result1 = eventAdapter.getPropertyValue("full name")
        val expected1 = TriggerValue("Lenon")

        // Assert
        assertNotNull(result1)
        assertEquals(expected1.stringValue(), result1.stringValue())
        assertNull(result1.numberValue())
        assertNull(result1.listValue())
        assertFalse { expected1.isList() }
    }

    @Test
    fun testGetPropertyPresentNormalisationsNormalisedMatch() {
        // Arrange
        val eventProperties = mapOf(
            "fullname" to "John",
            "full-name" to "Lennon",
            "age" to 30
        )
        val eventAdapter = EventAdapter("eventName", eventProperties)

        // Act
        val result = eventAdapter.getPropertyValue("full name")
        val expected = TriggerValue("John")

        // Assert
        assertNotNull(result)
        assertEquals(expected.stringValue(), result.stringValue())
        assertNull(result.numberValue())
        assertNull(result.listValue())
        assertFalse { expected.isList() }

        // Act
        val result1 = eventAdapter.getPropertyValue("FullName")
        val expected1 = TriggerValue("John")

        // Assert
        assertNotNull(result1)
        assertEquals(expected1.stringValue(), result1.stringValue())
        assertNull(result1.numberValue())
        assertNull(result1.listValue())
        assertFalse { expected1.isList() }
    }

    @Test
    fun testGetPropertyPresentNormalisationsNormalisedMatchBothSides() {
        // Arrange
        val eventProperties = mapOf(
            "FullName" to "John",
            "age" to 30
        )
        val eventAdapter = EventAdapter("eventName", eventProperties)

        // Act
        val result = eventAdapter.getPropertyValue("full name")
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
    fun testGetItemValuePresentNormalisation() {
        // Arrange
        val items = listOf(
            mapOf("i  temName" to "item1", "ite  mPrice" to 10),
            mapOf("itemN  ame" to "item2", "   itemPrice" to 20),
            mapOf("itemName     " to "item3", "itemPrice        " to 30),
            mapOf("itemNameInvalid" to " item33", "itemPriceInvalid        " to 330)
        )
        val eventAdapter = EventAdapter("eventName", emptyMap(), items)

        // Act
        val result1 = eventAdapter.getItemValue("itemName")

        // Assert
        assertNotNull(result1)
        result1.onEach {
            assertNull(it.numberValue())
            assertNull(it.listValue())
        }
        assertEquals(listOf("item1", "item2", "item3"), result1.map { it.stringValue() })

        // Act
        val result2 = eventAdapter.getItemValue("itemNameInvalid")

        // Assert
        assertNotNull(result2)
        result2.onEach {
            assertNull(it.numberValue())
            assertNull(it.listValue())
        }
        assertEquals(listOf(" item33"), result2.map { it.stringValue() })

        // Act
        val result3 = eventAdapter.getItemValue("itemPrice")

        // Assert
        assertNotNull(result3)
        result3.onEach {
            assertNull(it.stringValue())
            assertNull(it.listValue())
        }
        assertEquals(listOf(10, 20, 30), result3.map { it.numberValue() })

        // Act
        val result4 = eventAdapter.getItemValue("itemPriceInvalid")

        // Assert
        assertNotNull(result4)
        result4.onEach {
            assertNull(it.stringValue())
            assertNull(it.listValue())
        }
        assertEquals(listOf(330), result4.map { it.numberValue() })
    }

    @Test
    fun testGetItemValuePresentExactMatch() {
        // Arrange
        val items = listOf(
            mapOf("itemName" to "item1", "itemPrice" to 10),
            mapOf("item mame" to "item2", "itemPrice" to 20)
        )
        val eventAdapter = EventAdapter("itemName", emptyMap(), items)
        val expected = TriggerValue("item1")

        // Act
        val result = eventAdapter.getItemValue("itemName")

        // Assert
        assertNotNull(result)
        result.onEach {
            assertNull(it.numberValue())
            assertNull(it.listValue())
        }
        assertEquals(listOf("item1"), result.map { it.stringValue() })
        assertFalse(expected.isList())
    }

    @Test
    fun testGetItemValuePresentNormalisedMatch() {
        // Arrange
        val items = listOf(
            mapOf("itemName" to "item1", "itemPrice" to 10),
        )
        val eventAdapter = EventAdapter("item name", emptyMap(), items)
        val expected = TriggerValue("item1")

        // Act
        val result = eventAdapter.getItemValue("itemName")

        // Assert
        assertNotNull(result)
        result.onEach {
            assertNull(it.numberValue())
            assertNull(it.listValue())
        }
        assertEquals(listOf("item1"), result.map { it.stringValue() })
        assertFalse(expected.isList())
    }

    @Test
    fun testGetItemValuePresentNormalisedMatchBothWays() {
        // Arrange
        val items = listOf(
            mapOf("item name" to "item1", "itemPrice" to 10),
        )
        val eventAdapter = EventAdapter("ItemName", emptyMap(), items)
        val expected = TriggerValue("item1")

        // Act
        val result = eventAdapter.getItemValue("itemName")

        // Assert
        assertNotNull(result)
        result.onEach {
            assertNull(it.numberValue())
            assertNull(it.listValue())
        }
        assertEquals(listOf("item1"), result.map { it.stringValue() })
        assertFalse(expected.isList())
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
        assertEquals(emptyList(), result.map { it.stringValue() })
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