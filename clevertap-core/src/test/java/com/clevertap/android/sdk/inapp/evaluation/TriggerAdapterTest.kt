package com.clevertap.android.sdk.inapp.evaluation

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*

class TriggerAdapterTest {

    @Test
    fun testPropertyAtIndexValid() {
        // Arrange
        val triggerJSON = JSONObject()
        triggerJSON.put("eventName", "TestEvent")

        val propertiesArray = JSONArray()
        val propertyObject = JSONObject()
        propertyObject.put("propertyName", "Property1")
        propertyObject.put("operator", 1)
        propertyObject.put("propertyValue", "Value1")
        propertiesArray.put(propertyObject)

        triggerJSON.put("eventProperties", propertiesArray)

        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val triggerCondition = triggerAdapter.propertyAtIndex(0)

        // Assert
        assertEquals("Property1", triggerCondition?.propertyName)
        assertEquals(TriggerOperator.Equals, triggerCondition?.op)
        assertEquals("Value1", triggerCondition?.value?.stringValue())
    }

    @Test
    fun testPropertyAtIndexInvalidIndex() {
        // Arrange
        val triggerJSON = JSONObject()
        triggerJSON.put("eventName", "TestEvent")

        val propertiesArray = JSONArray()
        val propertyObject = JSONObject()
        propertyObject.put("propertyName", "Property1")
        propertyObject.put("operator", 1)
        propertyObject.put("propertyValue", "Value1")
        propertiesArray.put(propertyObject)

        triggerJSON.put("eventProperties", propertiesArray)

        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val triggerCondition = triggerAdapter.propertyAtIndex(1)

        // Assert
        assertNull(triggerCondition)
    }

    @Test
    fun testItemAtIndexValid() {
        // Arrange
        val triggerJSON = JSONObject()
        triggerJSON.put("eventName", "TestEvent")

        val itemsArray = JSONArray()
        val itemObject = JSONObject()
        itemObject.put("propertyName", "ItemProperty1")
        itemObject.put("operator", 2)
        itemObject.put("propertyValue", "ItemValue1")
        itemsArray.put(itemObject)

        triggerJSON.put("itemProperties", itemsArray)

        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val triggerCondition = triggerAdapter.itemAtIndex(0)

        // Assert
        assertEquals("ItemProperty1", triggerCondition?.propertyName)
        assertEquals(TriggerOperator.LessThan, triggerCondition?.op)
        assertEquals("ItemValue1", triggerCondition?.value?.stringValue())
    }

    @Test
    fun testItemAtIndexInvalidIndex() {
        // Arrange
        val triggerJSON = JSONObject()
        triggerJSON.put("eventName", "TestEvent")

        val itemsArray = JSONArray()
        val itemObject = JSONObject()
        itemObject.put("propertyName", "ItemProperty1")
        itemObject.put("operator", 2)
        itemObject.put("propertyValue", "ItemValue1")
        itemsArray.put(itemObject)

        triggerJSON.put("itemProperties", itemsArray)

        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val triggerCondition = triggerAdapter.itemAtIndex(1)

        // Assert
        assertNull(triggerCondition)
    }

    @Test
    fun testItemPropertiesNull() {
        // Arrange
        val triggerJSON = JSONObject()
        triggerJSON.put("eventName", "TestEvent")

        // 'itemProperties' is null
        triggerJSON.put("itemProperties", null)

        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val triggerCondition = triggerAdapter.itemAtIndex(0)

        // Assert
        assertNull(triggerCondition)
    }

    @Test
    fun testItemAtIndexObjectNull() {
        // Arrange
        val triggerJSON = JSONObject()
        triggerJSON.put("eventName", "TestEvent")

        val itemsArray = JSONArray()
        // Object at index 0 is null
        itemsArray.put(null)
        itemsArray.put(JSONObject()) // Valid item object

        triggerJSON.put("itemProperties", itemsArray)

        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val triggerCondition = triggerAdapter.itemAtIndex(0)

        // Assert
        assertNull(triggerCondition)
    }

    @Test
    fun testEventPropertiesNull() {
        // Arrange
        val triggerJSON = JSONObject()
        triggerJSON.put("eventName", "TestEvent")

        // 'eventProperties' is null
        triggerJSON.put("eventProperties", null)

        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val triggerCondition = triggerAdapter.propertyAtIndex(0)

        // Assert
        assertNull(triggerCondition)
    }

    @Test
    fun testPropertyAtIndexObjectNull() {
        // Arrange
        val triggerJSON = JSONObject()
        triggerJSON.put("eventName", "TestEvent")

        val propertiesArray = JSONArray()
        // Object at index 0 is null
        propertiesArray.put(null)
        propertiesArray.put(JSONObject()) // Valid property object

        triggerJSON.put("eventProperties", propertiesArray)

        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val triggerCondition = triggerAdapter.propertyAtIndex(0)

        // Assert
        assertNull(triggerCondition)
    }

    @Test
    fun testTriggerConditionFromJSON() {
        // Arrange
        val propertyObject = JSONObject()
        propertyObject.put("propertyValue", "TestValue")
        propertyObject.put(Constants.INAPP_OPERATOR, 1)
        propertyObject.put(Constants.INAPP_PROPERTYNAME, "propertyName")

        val triggerAdapter = TriggerAdapter(JSONObject())

        // Act
        val triggerCondition = triggerAdapter.triggerConditionFromJSON(propertyObject)

        // Assert
        assertEquals("propertyName", triggerCondition.propertyName)
        assertEquals(TriggerOperator.Equals, triggerCondition.op)
        assertEquals("TestValue", triggerCondition.value.stringValue())
    }

    @Test
    fun testPropertyCountWithNonNullProperties() {
        // Arrange
        val triggerJSON = JSONObject()
        val propertiesArray = JSONArray()
        propertiesArray.put(JSONObject())
        propertiesArray.put(JSONObject())
        triggerJSON.put("eventProperties", propertiesArray)
        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val propertyCount = triggerAdapter.propertyCount

        // Assert
        assertEquals(2, propertyCount)
    }

    @Test
    fun testPropertyCountWithNullProperties() {
        // Arrange
        val triggerJSON = JSONObject()
        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val propertyCount = triggerAdapter.propertyCount

        // Assert
        assertEquals(0, propertyCount)
    }

    @Test
    fun testItemsCountWithNonNullItems() {
        // Arrange
        val triggerJSON = JSONObject()
        val itemsArray = JSONArray()
        itemsArray.put(JSONObject())
        itemsArray.put(JSONObject())
        triggerJSON.put("itemProperties", itemsArray)
        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val itemsCount = triggerAdapter.itemsCount

        // Assert
        assertEquals(2, itemsCount)
    }

    @Test
    fun testItemsCountWithNullItems() {
        // Arrange
        val triggerJSON = JSONObject()
        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val itemsCount = triggerAdapter.itemsCount

        // Assert
        assertEquals(0, itemsCount)
    }

    @Test
    fun testEventNameWithNonNullEventName() {
        // Arrange
        val triggerJSON = JSONObject()
        triggerJSON.put("eventName", "SampleEventName")
        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val eventName = triggerAdapter.eventName

        // Assert
        assertEquals("SampleEventName", eventName)
    }

    @Test
    fun testEventNameWithNullEventName() {
        // Arrange
        val triggerJSON = JSONObject()
        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val eventName = triggerAdapter.eventName

        // Assert
        assertEquals("", eventName)
    }

    @Test
    fun testPropertiesWithNonNullProperties() {
        // Arrange
        val triggerJSON = JSONObject()
        val propertiesArray = JSONArray()
        propertiesArray.put(JSONObject())
        propertiesArray.put(JSONObject())
        triggerJSON.put("eventProperties", propertiesArray)
        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val properties = triggerAdapter.properties

        // Assert
        assertEquals(propertiesArray, properties)
    }

    @Test
    fun testPropertiesWithNullProperties() {
        // Arrange
        val triggerJSON = JSONObject()
        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val properties = triggerAdapter.properties

        // Assert
        assertEquals(null, properties)
    }

    @Test
    fun testItemsWithNonNullItems() {
        // Arrange
        val triggerJSON = JSONObject()
        val itemsArray = JSONArray()
        itemsArray.put(JSONObject())
        itemsArray.put(JSONObject())
        triggerJSON.put("itemProperties", itemsArray)
        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val items = triggerAdapter.items

        // Assert
        assertEquals(itemsArray, items)
    }

    @Test
    fun testItemsWithNullItems() {
        // Arrange
        val triggerJSON = JSONObject()
        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val items = triggerAdapter.items

        // Assert
        assertEquals(null, items)
    }

    @Test
    fun testOptTriggerOperatorWithNonNullValue() {
        // Arrange
        val jsonObject = JSONObject()
        jsonObject.put("operatorKey", TriggerOperator.GreaterThan.operatorValue)

        // Act
        val triggerOperator = jsonObject.optTriggerOperator("operatorKey")

        // Assert
        assertEquals(TriggerOperator.GreaterThan, triggerOperator)
    }

    @Test
    fun testOptTriggerOperatorWithNullValue() {
        // Arrange
        val jsonObject = JSONObject()

        // Act
        val triggerOperator = jsonObject.optTriggerOperator("nonExistentKey")

        // Assert
        assertEquals(TriggerOperator.Equals, triggerOperator)
    }
}
