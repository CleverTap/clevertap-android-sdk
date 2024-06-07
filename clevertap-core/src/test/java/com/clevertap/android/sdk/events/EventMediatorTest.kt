package com.clevertap.android.sdk.events

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.LocalDataStore
import com.clevertap.android.sdk.ProfileValueHandler
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EventMediatorTest {

    @MockK(relaxed = true)
    private lateinit var localDataStore: LocalDataStore

    @MockK
    private lateinit var profileValueHandler: ProfileValueHandler

    private lateinit var eventMediator: EventMediator

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        eventMediator = EventMediator(null, null, null, localDataStore, profileValueHandler)
    }

    @Test
    fun testGetChargedEventItemDetails() {
        val chargedEvtData = JSONObject()
        chargedEvtData.put(
            Constants.KEY_ITEMS, JSONArray(
                "[{\"product_id\": \"123\", \"product_name\": \"Sample Product 1\", \"quantity\": 2, \"price\": 49.99}," +
                        "{\"product_id\": \"456\", \"product_name\": \"Sample Product 2\", \"quantity\": 1, \"price\": 29.99}," +
                        "{\"product_id\": \"789\", \"product_name\": \"Sample Product 3\", \"quantity\": 3, \"price\": 99.99}]"
            )
        )
        chargedEvtData.put("property1", "value1")
        chargedEvtData.put("property2", 123)
        val sampleEvent = JSONObject().put(Constants.KEY_EVT_DATA, chargedEvtData)

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
        val chargedEvtData = JSONObject()
        chargedEvtData.put(
            Constants.KEY_ITEMS, JSONArray(
                "[{\"product_id\": \"123\", \"product_name\": \"Sample Product 1\", \"quantity\": 2, \"price\": 49.99}," +
                        "{\"product_id\": \"456\", \"product_name\": \"Sample Product 2\", \"quantity\": 1, \"price\": 29.99}," +
                        "{\"product_id\": \"789\", \"product_name\": \"Sample Product 3\", \"quantity\": 3, \"price\": 99.99}]"
            )
        )
        chargedEvtData.put("property1", "value1")
        chargedEvtData.put("property2", 123)
        val sampleEvent = JSONObject().put(Constants.KEY_EVT_DATA, chargedEvtData)

        val chargedDetails = eventMediator.getChargedEventDetails(sampleEvent)

        // Check if the charged details were correctly extracted
        val expectedDetails = HashMap<String, Any>()
        expectedDetails["property1"] = "value1"
        expectedDetails["property2"] = 123

        assertEquals(expectedDetails, chargedDetails)
    }

    @Test
    fun `test computeUserAttributeChangeProperties with skippable Props returns emptyMap`() {
        val profileEvent = JSONObject().put(
            Constants.PROFILE,
            JSONObject()
                .put("cc", "valueCC")
                .put("tz", "valueTZ")
                .put("Carrier", "valueCarrier")
        )

        val userAttributeChangeProperties = eventMediator.computeUserAttributeChangeProperties(profileEvent)


        verify { localDataStore.updateProfileFields(emptyMap()) }
        assertNull(userAttributeChangeProperties["cc"])
        assertNull(userAttributeChangeProperties["tz"])
        assertNull(userAttributeChangeProperties["carrier"])
        assertEquals(0, userAttributeChangeProperties.size)
    }

    @Test
    fun `test computeUserAttributeChangeProperties with basic props returns populated map`() {
        val profileEvent = JSONObject().put(
            Constants.PROFILE,
            JSONObject()
                .put("prop1", "value1")
                .put("prop2", 2)
                .put("prop3", true)
        )

        every { localDataStore.getProfileProperty("prop1") } returns "oldValue1"
        every { localDataStore.getProfileProperty("prop2") } returns 2
        every { localDataStore.getProfileProperty("prop3") } returns null

        val userAttributeChangeProperties = eventMediator.computeUserAttributeChangeProperties(profileEvent)

        val expectedDetails = HashMap<String, Map<String, Any>>()
        expectedDetails["prop1"] = mapOf("newValue" to "value1", "oldValue" to "oldValue1")
        expectedDetails["prop2"] = mapOf("newValue" to 2, "oldValue" to 2)
        expectedDetails["prop3"] = mapOf("newValue" to true)

        val expectedPersistenceUpdates = HashMap<String, Any>()
        expectedPersistenceUpdates["prop1"] = "value1"
        expectedPersistenceUpdates["prop2"] = 2
        expectedPersistenceUpdates["prop3"] = true

        val captor = slot<Map<String, Any>>()
        verify { localDataStore.updateProfileFields(capture(captor)) }
        assertEquals(expectedPersistenceUpdates, captor.captured)
        assertEquals(expectedDetails, userAttributeChangeProperties)
    }

    @Test
    fun `test computeUserAttributeChangeProperties with deletable props returns populated map`() {
        val profileEvent = JSONObject().put(
            Constants.PROFILE,
            JSONObject()
                .put("deleteProp", JSONObject().put(Constants.COMMAND_DELETE, true))
        )


        every { localDataStore.getProfileProperty("deleteProp") } returns "oldValue1"

        val userAttributeChangeProperties = eventMediator.computeUserAttributeChangeProperties(profileEvent)

        val expectedDetails = HashMap<String, Map<String, Any>>()
        expectedDetails["deleteProp"] = mapOf("oldValue" to "oldValue1")

        val captor = slot<Map<String, Any>>()
        verify { localDataStore.updateProfileFields(capture(captor)) }
        assertNull(captor.captured["deleteProp"])
        assertEquals(expectedDetails, userAttributeChangeProperties)
    }

    @Test
    fun `test computeUserAttributeChangeProperties with incr-decr props returns populated map`() {
        val profileEvent = JSONObject().put(
            Constants.PROFILE,
            JSONObject()
                .put("incrProp", JSONObject().put(Constants.COMMAND_INCREMENT, 10))
                .put("decrProp", JSONObject().put(Constants.COMMAND_DECREMENT, 10.5))

        )

        every { localDataStore.getProfileProperty("incrProp") } returns 10
        every { localDataStore.getProfileProperty("decrProp") } returns 20.5
        every { profileValueHandler.handleIncrementDecrementValues(10, Constants.COMMAND_INCREMENT, 10) } returns 20
        every {
            profileValueHandler.handleIncrementDecrementValues(
                10.5,
                Constants.COMMAND_DECREMENT,
                20.5
            )
        } returns 10.0

        val userAttributeChangeProperties = eventMediator.computeUserAttributeChangeProperties(profileEvent)

        val expectedDetails = HashMap<String, Map<String, Any>>()
        expectedDetails["incrProp"] = mapOf("newValue" to 20, "oldValue" to 10)
        expectedDetails["decrProp"] = mapOf("newValue" to 10.0, "oldValue" to 20.5)

        val expectedPersistenceUpdates = HashMap<String, Any>()
        expectedPersistenceUpdates["incrProp"] = 20
        expectedPersistenceUpdates["decrProp"] = 10.0

        val captor = slot<Map<String, Any>>()
        verify { localDataStore.updateProfileFields(capture(captor)) }
        assertEquals(expectedPersistenceUpdates, captor.captured)
        assertEquals(expectedDetails, userAttributeChangeProperties)
    }

    @Test
    fun `test computeUserAttributeChangeProperties with set-add-remove props returns populated map`() {
        val profileEvent = JSONObject().put(
            Constants.PROFILE,
            JSONObject()
                .put("setProp", JSONObject().put(Constants.COMMAND_SET, JSONArray().put("a").put("b")))
                .put("addProp", JSONObject().put(Constants.COMMAND_ADD, JSONArray().put("a").put("b")))
                .put("removeProp", JSONObject().put(Constants.COMMAND_REMOVE, JSONArray().put("a").put("b")))
        )

        every { localDataStore.getProfileProperty("setProp") } returns JSONArray().put("old")
        every { localDataStore.getProfileProperty("addProp") } returns JSONArray().put("old")
        every { localDataStore.getProfileProperty("removeProp") } returns JSONArray().put("a").put("b").put("old")

        every {
            profileValueHandler.handleMultiValues(
                "setProp",
                JSONArray().put("a").put("b"),
                Constants.COMMAND_SET,
                JSONArray().put("old")
            )
        } returns JSONArray().put("a").put("b")
        every {
            profileValueHandler.handleMultiValues(
                "addProp",
                JSONArray().put("a").put("b"),
                Constants.COMMAND_ADD,
                JSONArray().put("old")
            )
        } returns JSONArray().put("old").put("a").put("b")
        every {
            profileValueHandler.handleMultiValues(
                "removeProp",
                JSONArray().put("a").put("b"),
                Constants.COMMAND_REMOVE,
                JSONArray().put("a").put("b").put("old")
            )
        } returns JSONArray().put("old")

        val userAttributeChangeProperties = eventMediator.computeUserAttributeChangeProperties(profileEvent)

        val expectedDetails = HashMap<String, Map<String, Any>>()

        val expectedPersistenceUpdates = HashMap<String, Any>()
        expectedPersistenceUpdates["setProp"] = JSONArray().put("a").put("b")
        expectedPersistenceUpdates["addProp"] = JSONArray().put("old").put("a").put("b")
        expectedPersistenceUpdates["removeProp"] = JSONArray().put("old")

        val captor = slot<Map<String, Any>>()
        verify { localDataStore.updateProfileFields(capture(captor)) }
        assertEquals(setOf(expectedPersistenceUpdates["setProp"]), setOf(captor.captured["setProp"]))
        assertEquals(setOf(expectedPersistenceUpdates["addProp"]), setOf(captor.captured["addProp"]))
        assertEquals(setOf(expectedPersistenceUpdates["removeProp"]), setOf(captor.captured["removeProp"]))
        assertEquals(expectedDetails, userAttributeChangeProperties)
    }

    @Test
    fun `test computeUserAttributeChangeProperties with date props returns populated map`() {
        val profileEvent = JSONObject().put(
            Constants.PROFILE,
            JSONObject()
                .put("dateProp", "${Constants.DATE_PREFIX}1234")
        )


        every { localDataStore.getProfileProperty("dateProp") } returns 3456L

        val userAttributeChangeProperties = eventMediator.computeUserAttributeChangeProperties(profileEvent)

        val expectedDetails = HashMap<String, Map<String, Any>>()
        expectedDetails["dateProp"] = mapOf("newValue" to 1234L, "oldValue" to 3456L)

        val expectedPersistenceUpdates = HashMap<String, Any>()
        expectedPersistenceUpdates["dateProp"] = 1234L

        val captor = slot<Map<String, Any>>()
        verify { localDataStore.updateProfileFields(capture(captor)) }
        assertEquals(expectedPersistenceUpdates, captor.captured)
        assertEquals(expectedDetails, userAttributeChangeProperties)
    }

    @Test
    fun `test computeUserAttributeChangeProperties with empty profile returns empty map`() {
        val profileEvent = JSONObject().put(
            Constants.PROFILE,
            JSONObject()
        )

        val userAttributeChangeProperties = eventMediator.computeUserAttributeChangeProperties(profileEvent)
        val expectedDetails = HashMap<String, Map<String, Any>>()

        verify(exactly = 1) { localDataStore.updateProfileFields(emptyMap()) }
        assertEquals(expectedDetails, userAttributeChangeProperties)
    }

    @Test
    fun `test computeUserAttributeChangeProperties with non profile event returns empty map`() {
        val profileEvent = JSONObject().put(
            "abcd",
            JSONObject()
        )

        val userAttributeChangeProperties = eventMediator.computeUserAttributeChangeProperties(profileEvent)
        val expectedDetails = HashMap<String, Map<String, Any>>()

        verify(exactly = 0) { localDataStore.updateProfileFields(any()) }
        assertEquals(expectedDetails, userAttributeChangeProperties)
    }
}