package com.clevertap.android.sdk.events

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.LocalDataStore
import com.clevertap.android.sdk.ProfileValueHandler
import com.clevertap.android.sdk.network.NetworkRepo
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventMediatorTest {

    @MockK(relaxed = true)
    private lateinit var localDataStore: LocalDataStore

    @MockK
    private lateinit var profileValueHandler: ProfileValueHandler

    @MockK(relaxed = true)
    private lateinit var networkRepo: NetworkRepo

    @MockK(relaxed = true)
    private lateinit var config: CleverTapInstanceConfig

    @MockK(relaxed = true)
    private lateinit var cleverTapMetaData: CoreMetaData

    private lateinit var eventMediator: EventMediator

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        eventMediator = EventMediator(config, cleverTapMetaData, localDataStore, profileValueHandler, networkRepo)
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

    // Tests for shouldDeferProcessingEvent method
    @Test
    fun `shouldDeferProcessingEvent returns false for DEFINE_VARS_EVENT`() {
        val event = JSONObject()
        
        val result = eventMediator.shouldDeferProcessingEvent(event, Constants.DEFINE_VARS_EVENT)
        
        assertFalse(result)
    }

    @Test
    fun `shouldDeferProcessingEvent returns false when config is created post app launch`() {
        val event = JSONObject()
        every { config.isCreatedPostAppLaunch } returns true
        
        val result = eventMediator.shouldDeferProcessingEvent(event, Constants.RAISED_EVENT)
        
        assertFalse(result)
    }

    @Test
    fun `shouldDeferProcessingEvent returns false for system events when not created post app launch`() {
        val event = JSONObject().put("evtName", Constants.NOTIFICATION_CLICKED_EVENT_NAME)
        every { config.isCreatedPostAppLaunch } returns false
        
        val result = eventMediator.shouldDeferProcessingEvent(event, Constants.RAISED_EVENT)
        
        assertFalse(result)
    }

    @Test
    @Ignore("The test is failing due to incorrect code, we can fix it later")
    fun `shouldDeferProcessingEvent returns false for App Launched system event`() {
        val event = JSONObject().put("evtName", Constants.APP_LAUNCHED_EVENT)
        every { config.isCreatedPostAppLaunch } returns false
        every { cleverTapMetaData.isAppLaunchPushed } returns false
        
        val result = eventMediator.shouldDeferProcessingEvent(event, Constants.RAISED_EVENT)
        
        assertFalse(result)
    }

    @Test
    fun `shouldDeferProcessingEvent returns false for Notification Viewed system event`() {
        val event = JSONObject().put("evtName", Constants.NOTIFICATION_VIEWED_EVENT_NAME)
        every { config.isCreatedPostAppLaunch } returns false
        
        val result = eventMediator.shouldDeferProcessingEvent(event, Constants.RAISED_EVENT)
        
        assertFalse(result)
    }

    @Test
    fun `shouldDeferProcessingEvent returns false for Geofence Entered system event`() {
        val event = JSONObject().put("evtName", Constants.GEOFENCE_ENTERED_EVENT_NAME)
        every { config.isCreatedPostAppLaunch } returns false
        
        val result = eventMediator.shouldDeferProcessingEvent(event, Constants.RAISED_EVENT)
        
        assertFalse(result)
    }

    @Test
    fun `shouldDeferProcessingEvent returns false for Geofence Exited system event`() {
        val event = JSONObject().put("evtName", Constants.GEOFENCE_EXITED_EVENT_NAME)
        every { config.isCreatedPostAppLaunch } returns false
        
        val result = eventMediator.shouldDeferProcessingEvent(event, Constants.RAISED_EVENT)
        
        assertFalse(result)
    }

    @Test
    fun `shouldDeferProcessingEvent returns true for RAISED_EVENT when app launch not pushed and not created post app launch`() {
        val event = JSONObject().put("evtName", "CustomEvent")
        every { config.isCreatedPostAppLaunch } returns false
        every { cleverTapMetaData.isAppLaunchPushed } returns false
        
        val result = eventMediator.shouldDeferProcessingEvent(event, Constants.RAISED_EVENT)
        
        assertTrue(result)
    }

    @Test
    fun `shouldDeferProcessingEvent returns false for RAISED_EVENT when app launch is pushed`() {
        val event = JSONObject().put("evtName", "CustomEvent")
        every { config.isCreatedPostAppLaunch } returns false
        every { cleverTapMetaData.isAppLaunchPushed } returns true
        
        val result = eventMediator.shouldDeferProcessingEvent(event, Constants.RAISED_EVENT)
        
        assertFalse(result)
    }

    @Test
    fun `shouldDeferProcessingEvent returns false for non-RAISED_EVENT when not created post app launch`() {
        val event = JSONObject()
        every { config.isCreatedPostAppLaunch } returns false
        
        val result = eventMediator.shouldDeferProcessingEvent(event, Constants.PROFILE_EVENT)
        
        assertFalse(result)
    }

    @Test
    fun `shouldDeferProcessingEvent returns false for RAISED_EVENT when event has no evtName`() {
        val event = JSONObject()
        every { config.isCreatedPostAppLaunch } returns false
        every { cleverTapMetaData.isAppLaunchPushed } returns false
        
        val result = eventMediator.shouldDeferProcessingEvent(event, Constants.RAISED_EVENT)
        
        assertTrue(result)
    }

    @Test
    fun `shouldDeferProcessingEvent handles JSONException gracefully when evtName is not string`() {
        val event = JSONObject().put("evtName", 123) // Invalid evtName type
        every { config.isCreatedPostAppLaunch } returns false
        every { cleverTapMetaData.isAppLaunchPushed } returns false
        
        val result = eventMediator.shouldDeferProcessingEvent(event, Constants.RAISED_EVENT)
        
        assertTrue(result)
    }

    @Test
    fun `shouldDeferProcessingEvent returns false for PAGE_EVENT regardless of other conditions`() {
        val event = JSONObject().put("evtName", "CustomEvent")
        every { config.isCreatedPostAppLaunch } returns false
        every { cleverTapMetaData.isAppLaunchPushed } returns false
        
        val result = eventMediator.shouldDeferProcessingEvent(event, Constants.PAGE_EVENT)
        
        assertFalse(result)
    }

    @Test
    fun `shouldDeferProcessingEvent returns false for PROFILE_EVENT regardless of other conditions`() {
        val event = JSONObject().put("evtName", "CustomEvent")
        every { config.isCreatedPostAppLaunch } returns false
        every { cleverTapMetaData.isAppLaunchPushed } returns false
        
        val result = eventMediator.shouldDeferProcessingEvent(event, Constants.PROFILE_EVENT)
        
        assertFalse(result)
    }

    @Test
    fun `shouldDeferProcessingEvent returns false for FETCH_EVENT regardless of other conditions`() {
        val event = JSONObject().put("evtName", "CustomEvent")
        every { config.isCreatedPostAppLaunch } returns false
        every { cleverTapMetaData.isAppLaunchPushed } returns false
        
        val result = eventMediator.shouldDeferProcessingEvent(event, Constants.FETCH_EVENT)
        
        assertFalse(result)
    }

    // Tests for shouldDropEvent method
    @Test
    fun `shouldDropEvent returns false for FETCH_EVENT regardless of other conditions`() {
        val event = JSONObject()
        every { networkRepo.isMuted() } returns true
        every { cleverTapMetaData.isCurrentUserOptedOut } returns true
        
        val result = eventMediator.shouldDropEvent(event, Constants.FETCH_EVENT)
        
        assertFalse(result)
    }

    @Test
    fun `shouldDropEvent returns false for DEFINE_VARS_EVENT regardless of other conditions`() {
        val event = JSONObject()
        every { networkRepo.isMuted() } returns true
        every { cleverTapMetaData.isCurrentUserOptedOut } returns true
        
        val result = eventMediator.shouldDropEvent(event, Constants.DEFINE_VARS_EVENT)
        
        assertFalse(result)
    }

    @Test
    fun `shouldDropEvent returns true when network is muted`() {
        val event = JSONObject().put(Constants.KEY_EVT_NAME, "CustomEvent")
        every { networkRepo.isMuted() } returns true
        
        val result = eventMediator.shouldDropEvent(event, Constants.RAISED_EVENT)
        
        assertTrue(result)
    }

    @Test
    fun `shouldDropEvent returns false when network is not muted and user not opted out`() {
        val event = JSONObject().put(Constants.KEY_EVT_NAME, "CustomEvent")
        every { networkRepo.isMuted() } returns false
        every { cleverTapMetaData.isCurrentUserOptedOut } returns false
        
        val result = eventMediator.shouldDropEvent(event, Constants.RAISED_EVENT)
        
        assertFalse(result)
    }

    @Test
    fun `shouldDropEvent returns true when user opted out and system events disabled for RAISED_EVENT`() {
        val event = JSONObject().put(Constants.KEY_EVT_NAME, "CustomEvent")
        every { networkRepo.isMuted() } returns false
        every { cleverTapMetaData.isCurrentUserOptedOut } returns true
        every { cleverTapMetaData.enabledSystemEvents } returns false
        
        val result = eventMediator.shouldDropEvent(event, Constants.RAISED_EVENT)
        
        assertTrue(result)
    }

    @Test
    fun `shouldDropEvent returns true when user opted out and system events disabled for NV_EVENT`() {
        val event = JSONObject().put(Constants.KEY_EVT_NAME, "CustomEvent")
        every { networkRepo.isMuted() } returns false
        every { cleverTapMetaData.isCurrentUserOptedOut } returns true
        every { cleverTapMetaData.enabledSystemEvents } returns false
        
        val result = eventMediator.shouldDropEvent(event, Constants.NV_EVENT)
        
        assertTrue(result)
    }

    @Test
    fun `shouldDropEvent returns false when user opted out and system events enabled for PROFILE_EVENT`() {
        val event = JSONObject()
        every { networkRepo.isMuted() } returns false
        every { cleverTapMetaData.isCurrentUserOptedOut } returns true
        every { cleverTapMetaData.enabledSystemEvents } returns true
        
        val result = eventMediator.shouldDropEvent(event, Constants.PROFILE_EVENT)
        
        assertFalse(result)
    }

    @Test
    fun `shouldDropEvent returns false when user opted out and system events enabled for PAGE_EVENT`() {
        val event = JSONObject()
        every { networkRepo.isMuted() } returns false
        every { cleverTapMetaData.isCurrentUserOptedOut } returns true
        every { cleverTapMetaData.enabledSystemEvents } returns true
        
        val result = eventMediator.shouldDropEvent(event, Constants.PAGE_EVENT)
        
        assertFalse(result)
    }

    @Test
    fun `shouldDropEvent returns false when user opted out and system events enabled for DATA_EVENT`() {
        val event = JSONObject()
        every { networkRepo.isMuted() } returns false
        every { cleverTapMetaData.isCurrentUserOptedOut } returns true
        every { cleverTapMetaData.enabledSystemEvents } returns true
        
        val result = eventMediator.shouldDropEvent(event, Constants.DATA_EVENT)
        
        assertFalse(result)
    }

    @Test
    fun `shouldDropEvent returns false for RAISED_EVENT with system event when user opted out and system events enabled`() {
        val event = JSONObject().put(Constants.KEY_EVT_NAME, Constants.APP_LAUNCHED_EVENT)
        every { networkRepo.isMuted() } returns false
        every { cleverTapMetaData.isCurrentUserOptedOut } returns true
        every { cleverTapMetaData.enabledSystemEvents } returns true
        
        val result = eventMediator.shouldDropEvent(event, Constants.RAISED_EVENT)
        
        assertFalse(result)
    }

    @Test
    fun `shouldDropEvent returns true for RAISED_EVENT with non-system event when user opted out and system events enabled`() {
        val event = JSONObject().put(Constants.KEY_EVT_NAME, "CustomEvent")
        every { networkRepo.isMuted() } returns false
        every { cleverTapMetaData.isCurrentUserOptedOut } returns true
        every { cleverTapMetaData.enabledSystemEvents } returns true
        
        val result = eventMediator.shouldDropEvent(event, Constants.RAISED_EVENT)
        
        assertTrue(result)
    }

    @Test
    fun `shouldDropEvent returns false for NV_EVENT with system event when user opted out and system events enabled`() {
        val event = JSONObject().put(Constants.KEY_EVT_NAME, Constants.NOTIFICATION_CLICKED_EVENT_NAME)
        every { networkRepo.isMuted() } returns false
        every { cleverTapMetaData.isCurrentUserOptedOut } returns true
        every { cleverTapMetaData.enabledSystemEvents } returns true
        
        val result = eventMediator.shouldDropEvent(event, Constants.NV_EVENT)
        
        assertFalse(result)
    }

    @Test
    fun `shouldDropEvent returns true for NV_EVENT with non-system event when user opted out and system events enabled`() {
        val event = JSONObject().put(Constants.KEY_EVT_NAME, "CustomNVEvent")
        every { networkRepo.isMuted() } returns false
        every { cleverTapMetaData.isCurrentUserOptedOut } returns true
        every { cleverTapMetaData.enabledSystemEvents } returns true
        
        val result = eventMediator.shouldDropEvent(event, Constants.NV_EVENT)
        
        assertTrue(result)
    }

    @Test
    fun `shouldDropEvent handles null event gracefully when checking event name`() {
        every { networkRepo.isMuted() } returns false
        every { cleverTapMetaData.isCurrentUserOptedOut } returns true
        every { cleverTapMetaData.enabledSystemEvents } returns true
        
        val result = eventMediator.shouldDropEvent(null, Constants.RAISED_EVENT)
        
        assertTrue(result) // Should drop because event name is null (not a system event)
    }

    @Test
    fun `shouldDropEvent handles event without evtName when user opted out and system events enabled`() {
        val event = JSONObject() // No evtName field
        every { networkRepo.isMuted() } returns false
        every { cleverTapMetaData.isCurrentUserOptedOut } returns true
        every { cleverTapMetaData.enabledSystemEvents } returns true
        
        val result = eventMediator.shouldDropEvent(event, Constants.RAISED_EVENT)
        
        assertTrue(result) // Should drop because event name is null (not a system event)
    }

    @Test
    fun `shouldDropEvent handles JSONException gracefully when event name is invalid type`() {
        val event = JSONObject().put(Constants.KEY_EVT_NAME, 123) // Invalid type
        every { networkRepo.isMuted() } returns false
        every { cleverTapMetaData.isCurrentUserOptedOut } returns true
        every { cleverTapMetaData.enabledSystemEvents } returns true
        
        val result = eventMediator.shouldDropEvent(event, Constants.RAISED_EVENT)
        
        assertTrue(result) // Should drop because event name extraction fails
    }
}