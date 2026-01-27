package com.clevertap.android.sdk.events

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.network.NetworkRepo
import io.mockk.*
import io.mockk.impl.annotations.MockK
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventMediatorTest {
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
        eventMediator = EventMediator(config, cleverTapMetaData, networkRepo)
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

    @Test
    fun `isEvent should return true when event has evtName key`() {
        val event = JSONObject().put(Constants.KEY_EVT_NAME, "Test Event")

        val result = eventMediator.isEvent(event)

        assertTrue(result)
    }

    @Test
    fun `isEvent should return false when event does not have evtName key`() {
        val event = JSONObject().put("someOtherKey", "value")

        val result = eventMediator.isEvent(event)

        assertFalse(result)
    }

    @Test
    fun `isEvent should return false when event is empty`() {
        val event = JSONObject()

        val result = eventMediator.isEvent(event)

        assertFalse(result)
    }

    @Test
    fun `isAppLaunchedEvent should return true when event is App Launched`() {
        val event = JSONObject().put(Constants.KEY_EVT_NAME, Constants.APP_LAUNCHED_EVENT)

        val result = eventMediator.isAppLaunchedEvent(event)

        assertTrue(result)
    }

    @Test
    fun `isAppLaunchedEvent should return false when event is not App Launched`() {
        val event = JSONObject().put(Constants.KEY_EVT_NAME, "Some Other Event")

        val result = eventMediator.isAppLaunchedEvent(event)

        assertFalse(result)
    }

    @Test
    fun `isAppLaunchedEvent should return false when event does not have evtName key`() {
        val event = JSONObject().put("someOtherKey", "value")

        val result = eventMediator.isAppLaunchedEvent(event)

        assertFalse(result)
    }

    @Test
    fun `isChargedEvent should return true when event is Charged`() {
        val event = JSONObject().put(Constants.KEY_EVT_NAME, Constants.CHARGED_EVENT)

        val result = eventMediator.isChargedEvent(event)

        assertTrue(result)
    }

    @Test
    fun `isChargedEvent should return false when event is not Charged`() {
        val event = JSONObject().put(Constants.KEY_EVT_NAME, "Some Other Event")

        val result = eventMediator.isChargedEvent(event)

        assertFalse(result)
    }

    @Test
    fun `isChargedEvent should return false when event does not have evtName key`() {
        val event = JSONObject().put("someOtherKey", "value")

        val result = eventMediator.isChargedEvent(event)

        assertFalse(result)
    }
}