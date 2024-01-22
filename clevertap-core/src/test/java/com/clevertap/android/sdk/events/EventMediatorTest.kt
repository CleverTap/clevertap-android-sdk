package com.clevertap.android.sdk.events

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import kotlin.test.assertEquals

class EventMediatorTest {

    private var sampleEvent: JSONObject = JSONObject()

    @Before
    fun setup() {
        // Create a sample JSONObject for testing
        val evtData = JSONObject()
        evtData.put(
            Constants.KEY_ITEMS, JSONArray(
                "[{\"product_id\": \"123\", \"product_name\": \"Sample Product 1\", \"quantity\": 2, \"price\": 49.99}," +
                        "{\"product_id\": \"456\", \"product_name\": \"Sample Product 2\", \"quantity\": 1, \"price\": 29.99}," +
                        "{\"product_id\": \"789\", \"product_name\": \"Sample Product 3\", \"quantity\": 3, \"price\": 99.99}]"
            )
        )
        evtData.put("property1", "value1")
        evtData.put("property2", 123)
        sampleEvent.put(Constants.KEY_EVT_DATA, evtData)
    }

    @Test
    fun testGetChargedEventItemDetails() {
        val eventMediator = EventMediator(null, null, null)

        val itemDetails = eventMediator.getChargedEventItemDetails(sampleEvent)

        // Check if the list of item details was correctly extracted
        val expectedItemDetails = ArrayList<Map<String, Any>>()

        val product1 = HashMap<String, Any>()
        product1["product_id"] = "123"
        product1["product_name"] = "Sample Product 1"
        product1["quantity"] = 2
        product1["price"] = 49.99

        val product2 = HashMap<String, Any>()
        product2["product_id"] = "456"
        product2["product_name"] = "Sample Product 2"
        product2["quantity"] = 1
        product2["price"] = 29.99

        val product3 = HashMap<String, Any>()
        product3["product_id"] = "789"
        product3["product_name"] = "Sample Product 3"
        product3["quantity"] = 3
        product3["price"] = 99.99

        expectedItemDetails.add(product1)
        expectedItemDetails.add(product2)
        expectedItemDetails.add(product3)

        assertEquals(expectedItemDetails, itemDetails)
    }

    @Test
    fun testGetChargedEventDetails() {
        val yourClass = EventMediator(null, null, null)

        val chargedDetails = yourClass.getChargedEventDetails(sampleEvent)

        // Check if the charged details were correctly extracted
        val expectedDetails = HashMap<String, Any>()
        expectedDetails["property1"] = "value1"
        expectedDetails["property2"] = 123

        assertEquals(expectedDetails, chargedDetails)
    }
}