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
        triggerJSON.put(Constants.KEY_EVENT_NAME, "TestEvent")

        val propertiesArray = JSONArray()
        val propertyObject = JSONObject()

        propertyObject.put(Constants.INAPP_PROPERTYNAME, "Property1")
        propertyObject.put(Constants.INAPP_OPERATOR, 1)
        propertyObject.put(Constants.KEY_PROPERTY_VALUE, "Value1")
        propertiesArray.put(propertyObject)

        triggerJSON.put(Constants.KEY_EVENT_PROPERTIES, propertiesArray)

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
        triggerJSON.put(Constants.KEY_EVENT_NAME, "TestEvent")

        val propertiesArray = JSONArray()
        val propertyObject = JSONObject()
        propertyObject.put(Constants.INAPP_PROPERTYNAME, "Property1")
        propertyObject.put(Constants.INAPP_OPERATOR, 1)
        propertyObject.put(Constants.KEY_PROPERTY_VALUE, "Value1")
        propertiesArray.put(propertyObject)

        triggerJSON.put(Constants.KEY_EVENT_PROPERTIES, propertiesArray)

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
        triggerJSON.put(Constants.KEY_EVENT_NAME, "TestEvent")

        val itemsArray = JSONArray()
        val itemObject = JSONObject()
        itemObject.put(Constants.INAPP_PROPERTYNAME, "ItemProperty1")
        itemObject.put(Constants.INAPP_OPERATOR, 2)
        itemObject.put(Constants.KEY_PROPERTY_VALUE, "ItemValue1")
        itemsArray.put(itemObject)

        triggerJSON.put(Constants.KEY_ITEM_PROPERTIES, itemsArray)

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
        triggerJSON.put(Constants.KEY_EVENT_NAME, "TestEvent")

        val itemsArray = JSONArray()
        val itemObject = JSONObject()
        itemObject.put(Constants.INAPP_PROPERTYNAME, "ItemProperty1")
        itemObject.put(Constants.INAPP_OPERATOR, 2)
        itemObject.put(Constants.KEY_PROPERTY_VALUE, "ItemValue1")
        itemsArray.put(itemObject)

        triggerJSON.put(Constants.KEY_ITEM_PROPERTIES, itemsArray)

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
        triggerJSON.put(Constants.KEY_EVENT_NAME, "TestEvent")

        // 'itemProperties' is null
        triggerJSON.put(Constants.KEY_ITEM_PROPERTIES, null)

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
        triggerJSON.put(Constants.KEY_EVENT_NAME, "TestEvent")

        val itemsArray = JSONArray()
        // Object at index 0 is null
        itemsArray.put(null)
        itemsArray.put(JSONObject()) // Valid item object

        triggerJSON.put(Constants.KEY_ITEM_PROPERTIES, itemsArray)

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
        triggerJSON.put(Constants.KEY_EVENT_NAME, "TestEvent")

        // 'eventProperties' is null
        triggerJSON.put(Constants.KEY_EVENT_PROPERTIES, null)

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
        triggerJSON.put(Constants.KEY_EVENT_NAME, "TestEvent")

        val propertiesArray = JSONArray()
        // Object at index 0 is null
        propertiesArray.put(null)
        propertiesArray.put(JSONObject()) // Valid property object

        triggerJSON.put(Constants.KEY_EVENT_PROPERTIES, propertiesArray)

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
        propertyObject.put(Constants.INAPP_PROPERTYNAME, "propertyName")
        propertyObject.put(Constants.INAPP_OPERATOR, 1)
        propertyObject.put(Constants.KEY_PROPERTY_VALUE, "TestValue")

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
        triggerJSON.put(Constants.KEY_EVENT_PROPERTIES, propertiesArray)
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
        triggerJSON.put(Constants.KEY_ITEM_PROPERTIES, itemsArray)
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
        triggerJSON.put(Constants.KEY_EVENT_NAME, "SampleEventName")
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
    fun testProfileAttrNameWithNonNullProfileAttributeName() {
        // Arrange
        val triggerJSON = JSONObject()
        triggerJSON.put(Constants.KEY_PROFILE_ATTR_NAME, "SampleAttr")
        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val profileAttrName = triggerAdapter.profileAttrName

        // Assert
        assertEquals("SampleAttr", profileAttrName)
    }

    @Test
    fun testProfileAttrNameWithNonNullProfileAttributeNameNull() {
        // Arrange
        val triggerJSON = JSONObject()
        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val profileAttrName = triggerAdapter.profileAttrName

        // Assert
        assertNull(profileAttrName)
    }

    @Test
    fun testPropertiesWithNonNullProperties() {
        // Arrange
        val triggerJSON = JSONObject()
        val propertiesArray = JSONArray()
        propertiesArray.put(JSONObject())
        propertiesArray.put(JSONObject())
        triggerJSON.put(Constants.KEY_EVENT_PROPERTIES, propertiesArray)
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
        triggerJSON.put(Constants.KEY_ITEM_PROPERTIES, itemsArray)
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
        jsonObject.put(Constants.INAPP_OPERATOR, TriggerOperator.GreaterThan.operatorValue)

        // Act
        val triggerOperator = jsonObject.optTriggerOperator(Constants.INAPP_OPERATOR)

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

    @Test
    fun testGeoRadiusAtIndexValid() {
        // Arrange
        val triggerJSON = JSONObject()
        triggerJSON.put(Constants.KEY_EVENT_NAME, "TestEvent")

        val geoRadiusArray = JSONArray()
        val geoRadiusObject = JSONObject()
        geoRadiusObject.put("lat", 37.7749)
        geoRadiusObject.put("lng", -122.4194)
        geoRadiusObject.put("rad", 1000.0)
        geoRadiusArray.put(geoRadiusObject)

        triggerJSON.put(Constants.KEY_GEO_RADIUS_PROPERTIES, geoRadiusArray)

        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val geoRadius = triggerAdapter.geoRadiusAtIndex(0)

        // Assert
        assertNotNull(geoRadius)
        assertEquals(37.7749, geoRadius?.latitude)
        assertEquals(-122.4194, geoRadius?.longitude)
        assertEquals(1000.0, geoRadius?.radius)
    }

    @Test
    fun testGeoRadiusAtIndexInvalidIndex() {
        // Arrange
        val triggerJSON = JSONObject()
        triggerJSON.put(Constants.KEY_EVENT_NAME, "TestEvent")

        val geoRadiusArray = JSONArray()
        val geoRadiusObject = JSONObject()
        geoRadiusObject.put("lat", 37.7749)
        geoRadiusObject.put("lng", -122.4194)
        geoRadiusObject.put("rad", 1000.0)
        geoRadiusArray.put(geoRadiusObject)

        triggerJSON.put(Constants.KEY_GEO_RADIUS_PROPERTIES, geoRadiusArray)

        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val geoRadius = triggerAdapter.geoRadiusAtIndex(1)

        // Assert
        assertNull(geoRadius)
    }

    @Test
    fun testGeoRadiusAtIndexInvalidOutOfBoundIndex() {
        // Arrange
        val triggerJSON = JSONObject()
        triggerJSON.put(Constants.KEY_EVENT_NAME, "TestEvent")

        val geoRadiusArray = JSONArray()
        geoRadiusArray.put(JSONObject())
        geoRadiusArray.put(JSONObject())

        triggerJSON.put(Constants.KEY_GEO_RADIUS_PROPERTIES, geoRadiusArray)

        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val geoRadius = triggerAdapter.geoRadiusAtIndex(3)

        // Assert
        assertNull(geoRadius)
    }

    @Test
    fun testGeoRadiusArrayWithNonNullItems() {
        // Arrange
        val triggerJSON = JSONObject()
        val itemsArray = JSONArray()
        itemsArray.put(JSONObject())
        itemsArray.put(JSONObject())
        triggerJSON.put(Constants.KEY_GEO_RADIUS_PROPERTIES, itemsArray)
        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val items = triggerAdapter.geoRadiusArray

        // Assert
        assertEquals(itemsArray, items)
    }

    @Test
    fun testGeoRadiusArrayWithNullItems() {
        // Arrange
        val triggerJSON = JSONObject()
        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val items = triggerAdapter.geoRadiusArray

        // Assert
        assertEquals(null, items)
    }

    @Test
    fun testGeoRadiusCountWithNonNullGeoRadiusArray() {
        // Arrange
        val triggerJSON = JSONObject()
        val geoRadiusArray = JSONArray()
        geoRadiusArray.put(JSONObject())
        geoRadiusArray.put(JSONObject())
        triggerJSON.put(Constants.KEY_GEO_RADIUS_PROPERTIES, geoRadiusArray)
        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val geoRadiusCount = triggerAdapter.geoRadiusCount

        // Assert
        assertEquals(2, geoRadiusCount)
    }

    @Test
    fun testGeoRadiusCountWithNullGeoRadiusArray() {
        // Arrange
        val triggerJSON = JSONObject()
        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val geoRadiusCount = triggerAdapter.geoRadiusCount

        // Assert
        assertEquals(0, geoRadiusCount)
    }

    @Test
    fun testToJsonObject() {
        // Arrange
        val triggerJSON = JSONObject()
        triggerJSON.put(Constants.KEY_EVENT_NAME, "TestEvent")

        val propertiesArray = JSONArray()
        val propertyObject = JSONObject()
        propertyObject.put("propertyName", "Property1")
        propertyObject.put("operator", 1)
        propertyObject.put("value", "Value1")
        propertiesArray.put(propertyObject)

        triggerJSON.put(Constants.KEY_EVENT_PROPERTIES, propertiesArray)

        val itemsArray = JSONArray()
        val itemObject = JSONObject()
        itemObject.put(Constants.INAPP_PROPERTYNAME, "ItemProperty1")
        itemObject.put(Constants.INAPP_OPERATOR, 2)
        itemObject.put(Constants.KEY_PROPERTY_VALUE, "ItemValue1")
        itemsArray.put(itemObject)

        triggerJSON.put(Constants.KEY_ITEM_PROPERTIES, itemsArray)

        val geoRadiusArray = JSONArray()
        val geoRadiusObject = JSONObject()
        geoRadiusObject.put("lat", 37.7749)
        geoRadiusObject.put("lng", -122.4194)
        geoRadiusObject.put("rad", 1000.0)
        geoRadiusArray.put(geoRadiusObject)

        triggerJSON.put(Constants.KEY_GEO_RADIUS_PROPERTIES, geoRadiusArray)

        // Assert
        assertNotNull(triggerJSON)
        assertEquals("TestEvent", triggerJSON.optString(Constants.KEY_EVENT_NAME))
        assertEquals(propertiesArray, triggerJSON.optJSONArray(Constants.KEY_EVENT_PROPERTIES))
        assertEquals(itemsArray, triggerJSON.optJSONArray(Constants.KEY_ITEM_PROPERTIES))
        assertEquals(geoRadiusArray, triggerJSON.optJSONArray(Constants.KEY_GEO_RADIUS_PROPERTIES))
    }


    @Test
    fun `test firstTimeOnly with firstTimeOnly value as true`() {
        // Arrange
        val triggerJSON = JSONObject()
        triggerJSON.put(Constants.KEY_FIRST_TIME_ONLY, true)
        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val actualFirstTimeOnly = triggerAdapter.firstTimeOnly

        // Assert
        assertTrue(actualFirstTimeOnly)
    }

    @Test
    fun `test firstTimeOnly with firstTimeOnly value as false`(){
        // Arrange
        val triggerJSON = JSONObject()
        triggerJSON.put(Constants.KEY_FIRST_TIME_ONLY, false)
        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val actualFirstTimeOnly = triggerAdapter.firstTimeOnly

        // Assert
        assertFalse(actualFirstTimeOnly)
    }

    @Test
    fun `test firstTimeOnly with firstTimeOnly value as null`() {
        // Arrange
        val triggerJSON = JSONObject()
        val triggerAdapter = TriggerAdapter(triggerJSON)

        // Act
        val actualFirstTimeOnly = triggerAdapter.firstTimeOnly

        // Assert
        assertFalse(actualFirstTimeOnly)
    }

}
