package com.clevertap.android.sdk.inapp.evaluation

import android.location.Location
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.inapp.evaluation.TriggerOperator.Equals
import com.clevertap.android.sdk.inapp.store.preference.InAppStore
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.network.EndpointId
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.*
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.*
import org.hamcrest.beans.SamePropertyValuesAs.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EvaluationManagerTest : BaseTestCase() {

    private lateinit var triggersMatcher: TriggersMatcher
    private lateinit var triggersManager: TriggerManager
    private lateinit var limitsMatcher: LimitsMatcher
    private lateinit var evaluationManager: EvaluationManager
    private lateinit var storeRegistry: StoreRegistry

    override fun setUp() {
        super.setUp()
        MockKAnnotations.init(this)
        triggersMatcher = mockk(relaxed = true)
        triggersManager = mockk(relaxed = true)
        limitsMatcher = mockk(relaxed = true)
        storeRegistry = mockk(relaxed = true)
        evaluationManager = spyk(EvaluationManager(triggersMatcher, triggersManager, limitsMatcher, storeRegistry))
    }

    @Test
    fun `test evaluateOnEvent`() {
        // Arrange
        val eventName = "customEvent"
        val eventProperties = mapOf("key" to "value")
        val userLocation = mockk<Location>()

        // Capture the created EventAdapter
        val eventAdapterSlot = slot<List<EventAdapter>>()
        every { evaluationManager.evaluateServerSide(capture(eventAdapterSlot)) } returns Unit
        every { evaluationManager.evaluateClientSide(any()) } returns JSONArray().put(JSONObject(mapOf("resultKey" to "resultValue")))

        // Act
        val result = evaluationManager.evaluateOnEvent(eventName, eventProperties, userLocation)

        // Assert
        // Verify that the captured EventAdapter has the expected properties
        val capturedEventAdapter = eventAdapterSlot.captured[0]
        assertEquals(eventName, capturedEventAdapter.eventName)
        assertEquals(eventProperties, capturedEventAdapter.eventProperties)
        assertEquals(userLocation, capturedEventAdapter.userLocation)

        assertNotNull(result)
        assertTrue(result.length() > 0)

        // Perform more detailed assertions on the content of the JSONArray if needed
        val firstResultObject = result.getJSONObject(0)
        assertEquals("resultValue", firstResultObject.getString("resultKey"))

        verify(exactly = 1) { evaluationManager.evaluateServerSide(any()) }
        verify(exactly = 1) { evaluationManager.evaluateClientSide(any()) }
    }

    @Test
    fun `test evaluateOnAppLaunchedClientSide`() {
        // Arrange
        val userLocation = mockk<Location>()

        // Capture the created EventAdapter
        val eventAdapterSlot = slot<List<EventAdapter>>()
        every { evaluationManager.evaluateClientSide(capture(eventAdapterSlot)) } returns JSONArray().put(
            JSONObject(
                mapOf("resultKey" to "resultValue")
            )
        )

        // Act
        evaluationManager.evaluateOnAppLaunchedClientSide(emptyMap(), userLocation)

        // Assert
        // Verify that the captured EventAdapter has the expected properties for app launched event
        val capturedEventAdapter = eventAdapterSlot.captured[0]
        assertEquals(Constants.APP_LAUNCHED_EVENT, capturedEventAdapter.eventName)
        assertEquals(emptyMap(), capturedEventAdapter.eventProperties)
        assertEquals(userLocation, capturedEventAdapter.userLocation)

        verify(exactly = 1) { evaluationManager.evaluateClientSide(any()) }
        verify(exactly = 0) { evaluationManager.evaluateServerSide(any()) }
    }
    @Test
    fun `test evaluateOnChargedEvent`() {
        // Arrange
        val details = mapOf("key" to "value")
        val items = listOf(mapOf("itemKey" to "itemValue"))
        val userLocation = mockk<Location>()

        // Capture the created EventAdapter
        val eventAdapterSlot = slot<List<EventAdapter>>()
        every { evaluationManager.evaluateServerSide(capture(eventAdapterSlot)) } returns Unit
        every { evaluationManager.evaluateClientSide(any()) } returns JSONArray().put(JSONObject(mapOf("resultKey" to "resultValue")))

        // Act
        val result = evaluationManager.evaluateOnChargedEvent(details, items, userLocation)

        // Assert
        // Verify that the captured EventAdapter has the expected event type
        val capturedEventAdapter = eventAdapterSlot.captured[0]
        assertEquals(Constants.CHARGED_EVENT, capturedEventAdapter.eventName)

        assertNotNull(result)
        assertTrue(result.length() > 0)

        // Perform more detailed assertions on the content of the JSONArray if needed
        val firstResultObject = result.getJSONObject(0)
        assertEquals("resultValue", firstResultObject.getString("resultKey"))

        verify(exactly = 1) { evaluationManager.evaluateServerSide(any()) }
        verify(exactly = 1) { evaluationManager.evaluateClientSide(any()) }
    }

    @Test
    fun `evaluate should return empty list when inappNotifs is empty`() {
        // Arrange
        val event = EventAdapter("eventName", emptyMap(), userLocation = null)

        // Act
        val result = evaluationManager.evaluate(event, emptyList())

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun `evaluate should return empty list when no in-apps match triggers`() {
        // Arrange
        val event = EventAdapter("eventName", emptyMap(), userLocation = null)
        val inApp1 = JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, "campaign1")
        val inApp2 = JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, "campaign2")

        every { triggersMatcher.matchEvent(any(), any()) } returns false

        // Act
        val result = evaluationManager.evaluate(event, listOf(inApp1, inApp2))

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun `evaluate should return in-apps that match triggers and limits`() {
        // Arrange
        val event = EventAdapter("eventName", emptyMap(), userLocation = null)
        val inApp1 = JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, "campaign1")
        val inApp2 = JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, "campaign2")

        every { triggersMatcher.matchEvent(any(), any()) } returns true
        every { limitsMatcher.matchWhenLimits(any(), any()) } returns true

        // Act
        val result = evaluationManager.evaluate(event, listOf(inApp1, inApp2))

        // Assert
        assertEquals(2, result.size)
        assertEquals("campaign1", result[0].optString(Constants.INAPP_ID_IN_PAYLOAD))
        assertEquals("campaign2", result[1].optString(Constants.INAPP_ID_IN_PAYLOAD))
        // Verify that triggersManager.increment is called for each in-app
        verify(exactly = 2) { triggersManager.increment(any()) }
        // Verify that limitsMatcher.matchWhenLimits is called for each in-app
        verify(exactly = 2) { limitsMatcher.matchWhenLimits(any(), any()) }
    }

    @Test
    fun `evaluate should return in-apps that match triggers but not limits`() {
        // Arrange
        val event = EventAdapter("eventName", emptyMap(), userLocation = null)
        val inApp1 = JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, "campaign1")
        val inApp2 = JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, "campaign2")

        every { triggersMatcher.matchEvent(any(), any()) } returns true
        every { limitsMatcher.matchWhenLimits(any(), "campaign1") } returns true
        every { limitsMatcher.matchWhenLimits(any(), "campaign2") } returns false

        // Act
        val result = evaluationManager.evaluate(event, listOf(inApp1, inApp2))

        // Assert
        assertEquals(1, result.size)
        assertEquals("campaign1", result[0].optString(Constants.INAPP_ID_IN_PAYLOAD))
        // Verify that triggersManager.increment is called for the eligible in-app
        verify { triggersManager.increment("campaign1") }
        // Verify that limitsMatcher.matchWhenLimits is called for each in-app
        verify { limitsMatcher.matchWhenLimits(any(), "campaign1") }
        verify { limitsMatcher.matchWhenLimits(any(), "campaign2") }
    }

    @Test
    fun `evaluate should return empty list when in-apps match limits but not triggers`() {
        // Arrange
        val event = EventAdapter("eventName", emptyMap(), userLocation = null)
        every { triggersMatcher.matchEvent(any(), any()) } returns false
        every { limitsMatcher.matchWhenLimits(any(), "campaign1") } returns true
        every { limitsMatcher.matchWhenLimits(any(), "campaign2") } returns true

        // Act
        val result = evaluationManager.evaluate(event, listOf(JSONObject(), JSONObject()))

        // Assert
        assertEquals(0, result.size)
        // Verify that triggersManager.increment is not called
        verify(exactly = 0) { triggersManager.increment(any()) }
        // Verify that limitsMatcher.matchWhenLimits is called for each in-app
        verify(exactly = 0) { limitsMatcher.matchWhenLimits(any(), "campaign1") }
        verify(exactly = 0) { limitsMatcher.matchWhenLimits(any(), "campaign2") }
    }

    @Test
    fun `updateTTL should set TTL when offset is not null`() {
        // Arrange

        val inApp = JSONObject().put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 60L)

        // Act
        evaluationManager.updateTTL(inApp, FakeClock())

        // Assert
        assertEquals(70L, inApp.optLong(Constants.WZRK_TIME_TO_LIVE))
    }

    @Test
    fun `updateTTL should remove TTL when offset is null`() {
        // Arrange
        val inApp = JSONObject().put(Constants.WZRK_TIME_TO_LIVE_OFFSET, null)

        // Act
        evaluationManager.updateTTL(inApp)

        // Assert
        assertEquals(null, inApp.opt(Constants.WZRK_TIME_TO_LIVE))
    }

    @Test
    fun `updateTTL should not set TTL when offset is not a Long`() {
        // Arrange
        val inApp = JSONObject().put(Constants.WZRK_TIME_TO_LIVE_OFFSET, "not_a_long")

        // Act
        evaluationManager.updateTTL(inApp)

        // Assert
        assertEquals(null, inApp.opt(Constants.WZRK_TIME_TO_LIVE))
    }

    @Test
    fun `suppress should add entry to suppressedClientSideInApps with default values`() {
        // Arrange
        val inApp = JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, "campaign1")
        every { evaluationManager.generateWzrkId(any(), any()) } returns "campaign1_20231128"

        // Act
        evaluationManager.suppress(inApp)

        // Assert
        val expectedMap = mapOf(
            Constants.NOTIFICATION_ID_TAG to "campaign1_20231128",
            Constants.INAPP_WZRK_PIVOT to "wzrk_default",
            Constants.INAPP_WZRK_CGID to 0
        )
        assertEquals(expectedMap, evaluationManager.suppressedClientSideInApps.first())
    }

    @Test
    fun `suppress should add entry to suppressedClientSideInApps with custom values`() {
        // Arrange
        val inApp = JSONObject().apply {
            put(Constants.INAPP_ID_IN_PAYLOAD, "campaign2")
            put(Constants.INAPP_WZRK_PIVOT, "custom_pivot")
            put(Constants.INAPP_WZRK_CGID, 42)
        }
        every { evaluationManager.generateWzrkId(any(), any()) } returns "campaign2_20231128"

        // Act
        evaluationManager.suppress(inApp)

        // Assert
        val expectedMap = mapOf(
            Constants.NOTIFICATION_ID_TAG to "campaign2_20231128",
            Constants.INAPP_WZRK_PIVOT to "custom_pivot",
            Constants.INAPP_WZRK_CGID to 42
        )
        assertEquals(expectedMap, evaluationManager.suppressedClientSideInApps.first())
    }

    @Test
    fun `generateWzrkId should return formatted string with ti_date`() {
        // Arrange
        val ti = "campaign1"

        // Act
        val result = evaluationManager.generateWzrkId(ti, FakeClock())

        // Assert
        assertEquals("campaign1_20230126", result)
    }

    @Test
    fun `getWhenTriggers should return empty list when JSONArray is empty`() {
        // Arrange
        val triggerJson = JSONObject().put(Constants.INAPP_WHEN_TRIGGERS, JSONArray())

        // Act
        val result = evaluationManager.getWhenTriggers(triggerJson)

        // Assert
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getWhenTriggers should return list of TriggerAdapter objects`() {
        // Arrange
        val trigger1 = JSONObject().put("eventName", "event1")
        val trigger2 = JSONObject().put("eventName", "event2")

        val triggerJson = JSONObject().put(Constants.INAPP_WHEN_TRIGGERS, JSONArray().put(trigger1).put(trigger2))

        // Act
        val result = evaluationManager.getWhenTriggers(triggerJson)

        // Assert
        assertEquals(2, result.size)
        assertThat(TriggerAdapter(trigger1), samePropertyValuesAs(result[0]))
        assertThat(TriggerAdapter(trigger2), samePropertyValuesAs(result[1]))
    }

    @Test
    fun `getWhenTriggers should handle invalid JSONObject in JSONArray`() {
        // Arrange
        val triggerJson = JSONObject().put(Constants.INAPP_WHEN_TRIGGERS, JSONArray().put("invalidObject"))

        // Act
        val result = evaluationManager.getWhenTriggers(triggerJson)

        // Assert
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getWhenTriggers should return empty list when INAPP_WHEN_TRIGGERS is missing`() {
        // Arrange
        val triggerJson = JSONObject()

        // Act
        val result = evaluationManager.getWhenTriggers(triggerJson)

        // Assert
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getWhenTriggers should return empty list when INAPP_WHEN_TRIGGERS is not a JSONArray`() {
        // Arrange
        val triggerJson = JSONObject().put(Constants.INAPP_WHEN_TRIGGERS, "not_an_array")

        // Act
        val result = evaluationManager.getWhenTriggers(triggerJson)

        // Assert
        assertEquals(emptyList(), result)
    }

    @Test
    fun `getWhenTriggers should handle JSONArray with event properties`() {
        // Arrange
        val jsonString = """
            {
              "${Constants.INAPP_WHEN_TRIGGERS}": [
                {
                  "${Constants.KEY_EVT_NAME}": "TestEvent",
                  "eventProperties": [
                    {
                      "${Constants.INAPP_PROPERTYNAME}": "Property1",
                      "${Constants.INAPP_OPERATOR}": 1,
                      "${Constants.KEY_PROPERTY_VALUE}": "Value1"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        // Act
        val result = evaluationManager.getWhenTriggers(JSONObject(jsonString))

        // Assert
        assertEquals(1, result.size)
        assertEquals(1, result[0].propertyCount)

        val triggerCondition = result[0].propertyAtIndex(0)

        Assert.assertEquals("Property1", triggerCondition?.propertyName)
        Assert.assertEquals(Equals, triggerCondition?.op)
        Assert.assertEquals("Value1", triggerCondition?.value?.stringValue())
    }

    @Test
    fun `test evaluateClientSide if inapp suppressed then does not return after evaluation`() {
        val inApp1 = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 60L)
            .put(Constants.INAPP_SUPPRESSED, true)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val inApps = listOf(inApp1)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(any(), any()) } returns inApps

        val evaluateClientSide = evaluationManager.evaluateClientSide(listOf(EventAdapter("", mapOf())))

        assertEquals(0, evaluateClientSide.length())
        assertEquals(1, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateClientSide when 1st inapp is suppressed while other not`() {
        val inApp1 = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 60L)
            .put(Constants.INAPP_SUPPRESSED, true)
        val inApp2 = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 10L)
            .put(Constants.INAPP_SUPPRESSED, false)
        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val inApps = listOf(inApp1, inApp2)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(any(), any()) } returns inApps

        val evaluateClientSide = evaluationManager.evaluateClientSide(listOf(EventAdapter("", mapOf())))

        assertEquals(1, evaluateClientSide.length())
        assertEquals(1, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateClientSide when both inapps are not suppressed`() {
        val inApp1 = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 60L)
            .put(Constants.INAPP_SUPPRESSED, false)
        val inApp2 = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 10L)
            .put(Constants.INAPP_SUPPRESSED, false)
        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val inApps = listOf(inApp1, inApp2)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(any(), any()) } returns inApps

        val evaluateClientSide = evaluationManager.evaluateClientSide(listOf(EventAdapter("", mapOf())))

        assertEquals(1, evaluateClientSide.length())
        assertEquals(0, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateClientSide if inapp not suppressed then does return after evaluation`() {
        val inApp1 = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 20L)
            .put(Constants.INAPP_SUPPRESSED, false)
        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val inApps = listOf(inApp1)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(any(), any()) } returns inApps

        val evaluateClientSide = evaluationManager.evaluateClientSide(listOf(EventAdapter("", mapOf())))

        assertEquals(1, evaluateClientSide.length())
        assertEquals(0, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateOnAppLaunchedServerSide if inapp suppressed then does not return after evaluation`() {
        val inApp1 = JSONObject()
            .put(Constants.INAPP_SUPPRESSED, true)

        val inApps = listOf(inApp1)
        every { evaluationManager.evaluate(any(), any()) } returns inApps

        val evaluateClientSide = evaluationManager.evaluateOnAppLaunchedServerSide(inApps, emptyMap(), null)

        assertEquals(0, evaluateClientSide.length())
        assertEquals(1, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateOnAppLaunchedServerSide when 1st inapp is suppressed while other not`() {
        val inApp1 = JSONObject()
            .put(Constants.INAPP_SUPPRESSED, true)
        val inApp2 = JSONObject()
            .put(Constants.INAPP_SUPPRESSED, false)
        val inApps = listOf(inApp1, inApp2)
        every { evaluationManager.evaluate(any(), any()) } returns inApps

        val evaluateClientSide = evaluationManager.evaluateOnAppLaunchedServerSide(inApps, emptyMap(), null)

        assertEquals(1, evaluateClientSide.length())
        assertEquals(1, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateOnAppLaunchedServerSide when both inapps are not suppressed`() {
        val inApp1 = JSONObject()
            .put(Constants.INAPP_SUPPRESSED, false)
        val inApp2 = JSONObject()
            .put(Constants.INAPP_SUPPRESSED, false)
        val inApps = listOf(inApp1, inApp2)
        every { evaluationManager.evaluate(any(), any()) } returns inApps

        val evaluateClientSide = evaluationManager.evaluateOnAppLaunchedServerSide(inApps, emptyMap(), null)

        assertEquals(1, evaluateClientSide.length())
        assertEquals(0, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateOnAppLaunchedServerSide if inapp not suppressed then does return after evaluation`() {
        val inApp1 = JSONObject()
            .put(Constants.INAPP_SUPPRESSED, false)
        val inApps = listOf(inApp1)
        every { evaluationManager.evaluate(any(), any()) } returns inApps

        val evaluateClientSide = evaluationManager.evaluateOnAppLaunchedServerSide(inApps, emptyMap(), null)

        assertEquals(1, evaluateClientSide.length())
        assertEquals(0, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateServerSide when campaignId is 0 then don't add id to evaluatedServerSideCampaignIds`() {
        val inApp1 = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, 0)
        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val inApps = listOf(inApp1)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(any(), any()) } returns inApps

        evaluationManager.evaluateServerSide(listOf(EventAdapter("", mapOf())))

        assertEquals(0, evaluationManager.evaluatedServerSideCampaignIds.size)
    }

    @Test
    fun `test evaluateServerSide when campaignId is not 0 then add id to evaluatedServerSideCampaignIds`() {
        val inApp1 = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, 123)
        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val inApps = listOf(inApp1)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(any(), any()) } returns inApps

        evaluationManager.evaluateServerSide(listOf(EventAdapter("", mapOf())))

        assertEquals(1, evaluationManager.evaluatedServerSideCampaignIds.size)
    }

    @Test
    fun `test sortByPriority with valid priorities and timestamps`() {
        val jsonObject1 = JSONObject()
        jsonObject1.put("priority", 3)
        jsonObject1.put(Constants.INAPP_ID_IN_PAYLOAD, "2023-09-14T10:30:00")

        val jsonObject2 = JSONObject()
        jsonObject2.put("priority", 1)
        jsonObject2.put(Constants.INAPP_ID_IN_PAYLOAD, "2023-09-15T08:45:00")

        val jsonObject3 = JSONObject()
        jsonObject3.put("priority", 2)
        jsonObject3.put(Constants.INAPP_ID_IN_PAYLOAD, "2023-09-16T12:15:00")

        val inApps = listOf(jsonObject1, jsonObject2, jsonObject3)
        val sortedList = evaluationManager.sortByPriority(inApps)

        val expectedOrder = listOf(jsonObject1, jsonObject3, jsonObject2)
        assertThat(sortedList, `is`(expectedOrder))
    }

    @Test
    fun `test sortByPriority with missing priority field`() {
        val jsonObject1 = JSONObject()
        jsonObject1.put(Constants.INAPP_ID_IN_PAYLOAD, "2023-09-14T15:20:00")

        val jsonObject2 = JSONObject()
        jsonObject2.put("priority", 5)
        jsonObject2.put(Constants.INAPP_ID_IN_PAYLOAD, "2023-09-15T14:00:00")

        val inApps = listOf(jsonObject1, jsonObject2)
        val sortedList = evaluationManager.sortByPriority(inApps)

        val expectedOrder = listOf(jsonObject2, jsonObject1)
        assertThat(sortedList, `is`(expectedOrder))
    }

    @Test
    fun `test sortByPriority with empty input list`() {
        val inApps = emptyList<JSONObject>()
        val sortedList = evaluationManager.sortByPriority(inApps)

        assertThat(sortedList, `is`(emptyList()))
    }

    @Test
    fun `test sortByPriority with equal priority but different timestamps`() {
        val jsonObject1 = JSONObject()
        jsonObject1.put("priority", 3)
        jsonObject1.put(Constants.INAPP_ID_IN_PAYLOAD, "1631615400000")  // Milliseconds format

        val jsonObject2 = JSONObject()
        jsonObject2.put("priority", 3)
        jsonObject2.put(Constants.INAPP_ID_IN_PAYLOAD, "1631619000000")  // Different timestamp

        val inApps = listOf(jsonObject1, jsonObject2)
        val sortedList = evaluationManager.sortByPriority(inApps)

        val expectedOrder =
            listOf(jsonObject1, jsonObject2)  // Sorted by priority (timestamp doesn't affect order)
        assertThat(sortedList, `is`(expectedOrder))
    }

    @Test
    fun `test sortByPriority with missing timestamp in one JSONObject`() {
        val jsonObject1 = JSONObject()
        jsonObject1.put("priority", 2)
        jsonObject1.put(Constants.INAPP_ID_IN_PAYLOAD, "1695208020000")

        val jsonObject2 = JSONObject()
        jsonObject2.put("priority", 3)  // Higher priority
        // Timestamp missing in jsonObject2

        val inApps = listOf(jsonObject1, jsonObject2)
        val sortedList = evaluationManager.sortByPriority(inApps)

        val expectedOrder =
            listOf(jsonObject2, jsonObject1)  // Sorted by priority (2nd has higher priority)
        assertThat(sortedList, `is`(expectedOrder))
    }

    @Test
    fun `test sortByPriority with equal priority and missing timestamp in-app is created first`() {
        val jsonObject1 = JSONObject()
        jsonObject1.put("priority", 2)
        jsonObject1.put(
            Constants.INAPP_ID_IN_PAYLOAD,
            "" + (Clock.SYSTEM.newDate().time + 10_000)
        )  // Milliseconds format

        val jsonObject2 = JSONObject()
        jsonObject2.put("priority", 2)  // Equal priority
        // Timestamp missing in jsonObject2

        val inApps = listOf(jsonObject1, jsonObject2)
        val sortedList = evaluationManager.sortByPriority(inApps)

        val expectedOrder = listOf(
            jsonObject2,
            jsonObject1
        )  // Sorted by priority (equal priority, timestamp missing)
        assertThat(sortedList, `is`(expectedOrder))
    }

    @Test
    fun `test sortByPriority with equal priority and missing timestamp in-app is created second`() {
        val jsonObject1 = JSONObject()
        jsonObject1.put("priority", 2)
        jsonObject1.put(
            Constants.INAPP_ID_IN_PAYLOAD,
            "" + (Clock.SYSTEM.newDate().time - 60_000)
        )  // Milliseconds format

        val jsonObject2 = JSONObject()
        jsonObject2.put("priority", 2)  // Equal priority
        // Timestamp missing in jsonObject2

        val inApps = listOf(jsonObject1, jsonObject2)
        val sortedList = evaluationManager.sortByPriority(inApps)

        val expectedOrder = listOf(
            jsonObject1,
            jsonObject2
        )  // Sorted by priority (equal priority, timestamp missing)
        assertThat(sortedList, `is`(expectedOrder))
    }

    @Test
    fun `test sortByPriority with equal timestamp and different priorities`() {
        val jsonObject1 = JSONObject()
        jsonObject1.put("priority", 2)
        jsonObject1.put(Constants.INAPP_ID_IN_PAYLOAD, "1631615400000")  // Milliseconds format

        val jsonObject2 = JSONObject()
        jsonObject2.put("priority", 3)  // Higher priority
        jsonObject2.put(Constants.INAPP_ID_IN_PAYLOAD, "1631615400000")  // Same timestamp

        val inApps = listOf(jsonObject1, jsonObject2)
        val sortedList = evaluationManager.sortByPriority(inApps)

        val expectedOrder =
            listOf(jsonObject2, jsonObject1)  // Sorted by priority (higher priority first)
        assertThat(sortedList, `is`(expectedOrder))
    }

    @Test
    fun `test sortByPriority with equal timestamp and missing priority`() {
        val jsonObject1 = JSONObject()
        jsonObject1.put("priority", 2)
        jsonObject1.put(Constants.INAPP_ID_IN_PAYLOAD, "1631615400000")  // Milliseconds format

        val jsonObject2 = JSONObject()
        // Priority missing in jsonObject2
        jsonObject2.put(Constants.INAPP_ID_IN_PAYLOAD, "1631615400000")  // Same timestamp

        val inApps = listOf(jsonObject1, jsonObject2)
        val sortedList = evaluationManager.sortByPriority(inApps)

        val expectedOrder =
            listOf(jsonObject1, jsonObject2)  // Sorted by priority (missing priority comes after)
        assertThat(sortedList, `is`(expectedOrder))
    }

    @Test
    fun `test sortByPriority with a single JSONObject`() {
        val jsonObject = JSONObject()
        jsonObject.put("priority", 3)
        jsonObject.put(Constants.INAPP_ID_IN_PAYLOAD, "1631615400000")  // Milliseconds format

        val inApps = listOf(jsonObject)
        val sortedList = evaluationManager.sortByPriority(inApps)

        assertThat(sortedList, `is`(inApps))
    }

    @Test
    fun `test getWhenLimits with valid input`() {
        val limitJSON = JSONObject()
        val fL1 = JSONObject().apply {
            put("type", "minutes")
            put("limit", 10)
            put("frequency", 30)
        }
        val oL1 = JSONObject().apply {
            put("type", "onExactly")
            put("limit", 1)
        }
        limitJSON.put("frequencyLimits", JSONArray().put(fL1))
        limitJSON.put("occurrenceLimits", JSONArray().put(oL1))

        val result = evaluationManager.getWhenLimits(limitJSON)

        assertEquals(2, result.size)
        // Adjust the assertions based on the actual implementation of LimitAdapter
        assertThat(
            LimitAdapter(fL1),
            samePropertyValuesAs(result[0])
        )
        assertThat(
            LimitAdapter(oL1),
            samePropertyValuesAs(result[1])
        )
    }

    @Test
    fun `test getWhenLimits with empty JSON arrays`() {
        val limitJSON = JSONObject()
        limitJSON.put("frequencyLimits", JSONArray())
        limitJSON.put("occurrenceLimits", JSONArray())

        val result = evaluationManager.getWhenLimits(limitJSON)

        assertEquals(0, result.size)
    }

    @Test
    fun `test getWhenLimits with empty JSON object in json arrays`() {
        val limitJSON = JSONObject()
        limitJSON.put("frequencyLimits", JSONArray().put(JSONObject()))
        limitJSON.put("occurrenceLimits", JSONArray().put(JSONObject()))

        val result = evaluationManager.getWhenLimits(limitJSON)

        assertEquals(0, result.size)
    }

    @Test
    fun `onAttachHeaders returns JSONObject when evaluatedServerSideCampaignIds is not empty`() {
        // Arrange
        val endpointId = EndpointId.ENDPOINT_A1
        // Populate evaluatedServerSideCampaignIds with some values
        evaluationManager.evaluatedServerSideCampaignIds.add(1)

        // Act
        val result = evaluationManager.onAttachHeaders(endpointId)

        // Assert
        assertNotNull(result)
        assertTrue(result.has(Constants.INAPP_SS_EVAL_META))
        val evaluatedIdsArray = result.getJSONArray(Constants.INAPP_SS_EVAL_META)
        assertEquals(1, evaluatedIdsArray.length())
        assertEquals(1L, evaluatedIdsArray.getLong(0))

        assertFalse(result.has(Constants.INAPP_SUPPRESSED_META)) // Ensure the other key is not present
    }

    @Test
    fun `onAttachHeaders returns JSONObject when suppressedClientSideInApps is not empty`() {
        // Arrange
        val endpointId = EndpointId.ENDPOINT_A1
        evaluationManager.suppressedClientSideInApps.add(mapOf("key" to "value"))

        // Act
        val result = evaluationManager.onAttachHeaders(endpointId)

        // Assert
        assertNotNull(result)

        assertFalse(result.has(Constants.INAPP_SS_EVAL_META)) // Ensure the other key is not present

        assertTrue(result.has(Constants.INAPP_SUPPRESSED_META))
        val suppressedAppsArray = result.getJSONArray(Constants.INAPP_SUPPRESSED_META)
        assertEquals(1, suppressedAppsArray.length())
        val suppressedApp = suppressedAppsArray.getJSONObject(0)
        assertEquals("value", suppressedApp.getString("key"))
    }

    @Test
    fun `onAttachHeaders returns JSONObject when both lists are not empty`() {
        // Arrange
        val endpointId = EndpointId.ENDPOINT_A1
        evaluationManager.suppressedClientSideInApps.add(mapOf("key" to "value"))
        evaluationManager.evaluatedServerSideCampaignIds.add(1)

        // Act
        val result = evaluationManager.onAttachHeaders(endpointId)

        // Assert
        assertNotNull(result)

        assertTrue(result.has(Constants.INAPP_SS_EVAL_META))
        assertTrue(result.has(Constants.INAPP_SUPPRESSED_META))

        val suppressedAppsArray = result.getJSONArray(Constants.INAPP_SUPPRESSED_META)
        assertEquals(1, suppressedAppsArray.length())
        val suppressedApp = suppressedAppsArray.getJSONObject(0)
        assertEquals("value", suppressedApp.getString("key"))

        val evaluatedIdsArray = result.getJSONArray(Constants.INAPP_SS_EVAL_META)
        assertEquals(1, evaluatedIdsArray.length())
        assertEquals(1L, evaluatedIdsArray.getLong(0))
    }

    @Test
    fun `onAttachHeaders returns null when both lists are empty`() {
        // Arrange
        val endpointId = EndpointId.ENDPOINT_A1

        // Act
        val result = evaluationManager.onAttachHeaders(endpointId)

        // Assert
        assertNull(result)
    }

    @Test
    fun `onAttachHeaders returns null for non-ENDPOINT_A1`() {
        // Arrange
        val endpointId = EndpointId.ENDPOINT_HELLO

        evaluationManager.suppressedClientSideInApps.add(mapOf("key" to "value"))
        evaluationManager.evaluatedServerSideCampaignIds.add(1)

        // Act
        val result = evaluationManager.onAttachHeaders(endpointId)

        // Assert
        assertNull(result)
    }

    @Test
    fun `onSentHeaders removes sent evaluatedServerSideCampaignIds and suppressedClientSideInApps for ENDPOINT_A1`() {
        // Arrange
        val endpointId = EndpointId.ENDPOINT_A1

        // Create a JSONObject with the desired structure
        val header = JSONObject().apply {
            put(Constants.INAPP_SS_EVAL_META, JSONArray().put(1).put(2).put(3))
            put(
                Constants.INAPP_SUPPRESSED_META,
                JSONArray().put(mapOf(Constants.NOTIFICATION_ID_TAG to "id1"))
                    .put(mapOf(Constants.NOTIFICATION_ID_TAG to "id2"))
            )
        }

        // Manually manipulate the evaluatedServerSideCampaignIds and suppressedClientSideInApps lists
        evaluationManager.evaluatedServerSideCampaignIds.add(1L)
        evaluationManager.evaluatedServerSideCampaignIds.add(2L)
        evaluationManager.evaluatedServerSideCampaignIds.add(3L)

        evaluationManager.suppressedClientSideInApps.add(mapOf(Constants.NOTIFICATION_ID_TAG to "id1"))
        evaluationManager.suppressedClientSideInApps.add(mapOf(Constants.NOTIFICATION_ID_TAG to "id2"))
        evaluationManager.suppressedClientSideInApps.add(mapOf(Constants.NOTIFICATION_ID_TAG to "id3"))

        // Act
        evaluationManager.onSentHeaders(header, endpointId)

        // Assert
        // Manually assert the state after the method call
        assertTrue(evaluationManager.evaluatedServerSideCampaignIds.isEmpty())

        val resultList = evaluationManager.suppressedClientSideInApps
        assertEquals(1, resultList.size)
        assertEquals("id3", resultList[0][Constants.NOTIFICATION_ID_TAG])
    }

    @Test
    fun `onSentHeaders doesn't remove sent suppressedClientSideInApps for ENDPOINT_A1 when NOTIFICATION_ID_TAG not present in header json`() {
        // Arrange
        val endpointId = EndpointId.ENDPOINT_A1

        // Create a JSONObject with the desired structure
        val header = JSONObject().apply {
            put(
                Constants.INAPP_SUPPRESSED_META,
                JSONArray().put(mapOf("key" to "12"))
            )
        }

        evaluationManager.suppressedClientSideInApps.add(mapOf(Constants.NOTIFICATION_ID_TAG to "112322222_646464646"))

        // Act
        evaluationManager.onSentHeaders(header, endpointId)

        // Assert

        val resultList = evaluationManager.suppressedClientSideInApps
        assertEquals(1, resultList.size)
        assertEquals("112322222_646464646", resultList[0][Constants.NOTIFICATION_ID_TAG])
    }

    @Test
    fun `onSentHeaders removes sent suppressedClientSideInApps for ENDPOINT_A1 when key in header json is different but id is same`() {
        // Arrange
        val endpointId = EndpointId.ENDPOINT_A1

        // Create a JSONObject with the desired structure
        val header = JSONObject().apply {
            put(
                Constants.INAPP_SUPPRESSED_META,
                JSONArray().put(mapOf("key" to "112322222_646464646"))// key is not wzrk_id but value matches
            )
        }

        evaluationManager.suppressedClientSideInApps.add(mapOf(Constants.NOTIFICATION_ID_TAG to "112322222_646464646"))

        // Act
        evaluationManager.onSentHeaders(header, endpointId)

        // Assert

        val resultList = evaluationManager.suppressedClientSideInApps
        assertEquals(0, resultList.size)
    }

    @Test
    fun `onSentHeaders doesn't remove sent suppressedClientSideInApps for non-ENDPOINT_A1`() {
        // Arrange
        val endpointId = EndpointId.ENDPOINT_HELLO

        // Create a JSONObject with the desired structure
        val header = JSONObject().apply {
            put(
                Constants.INAPP_SUPPRESSED_META,
                JSONArray().put(mapOf(Constants.NOTIFICATION_ID_TAG to "112322222_646464646"))// key is not wzrk_id but value matches
            )
        }

        evaluationManager.suppressedClientSideInApps.add(mapOf(Constants.NOTIFICATION_ID_TAG to "112322222_646464646"))

        // Act
        evaluationManager.onSentHeaders(header, endpointId)

        // Assert

        val resultList = evaluationManager.suppressedClientSideInApps
        assertEquals(1, resultList.size)
        assertEquals("112322222_646464646", resultList[0][Constants.NOTIFICATION_ID_TAG])
    }

    @Test
    fun `onSentHeaders doesn't remove sent evaluatedServerSideCampaignIds for non-ENDPOINT_A1`() {
        // Arrange
        val endpointId = EndpointId.ENDPOINT_HELLO

        val header = JSONObject().apply {
            put(Constants.INAPP_SS_EVAL_META, JSONArray().put(1))
        }

        evaluationManager.evaluatedServerSideCampaignIds.add(1L)

        // Act
        evaluationManager.onSentHeaders(header, endpointId)

        // Assert

        val resultList = evaluationManager.evaluatedServerSideCampaignIds
        assertEquals(1, resultList.size)
    }

    @Test
    fun `onSentHeaders doesn't remove sent suppressedClientSideInApps for empty header`() {
        // Arrange
        val endpointId = EndpointId.ENDPOINT_A1

        // Create a JSONObject with the desired structure
        val header = JSONObject()

        evaluationManager.suppressedClientSideInApps.add(mapOf(Constants.NOTIFICATION_ID_TAG to "112322222_646464646"))

        // Act
        evaluationManager.onSentHeaders(header, endpointId)

        // Assert

        val resultList = evaluationManager.suppressedClientSideInApps
        assertEquals(1, resultList.size)
        assertEquals("112322222_646464646", resultList[0][Constants.NOTIFICATION_ID_TAG])
    }

    @Test
    fun `onSentHeaders doesn't remove sent suppressedClientSideInApps for empty header json array`() {
        // Arrange
        val endpointId = EndpointId.ENDPOINT_A1

        // Create a JSONObject with the desired structure
        val header = JSONObject().put(Constants.INAPP_SUPPRESSED_META, JSONArray())

        evaluationManager.suppressedClientSideInApps.add(mapOf(Constants.NOTIFICATION_ID_TAG to "112322222_646464646"))

        // Act
        evaluationManager.onSentHeaders(header, endpointId)

        // Assert

        val resultList = evaluationManager.suppressedClientSideInApps
        assertEquals(1, resultList.size)
        assertEquals("112322222_646464646", resultList[0][Constants.NOTIFICATION_ID_TAG])
    }

    @Test
    fun `onSentHeaders should not crash when suppressedClientSideInApps doesn't have NOTIFICATION_ID_TAG`() {
        // Arrange
        val endpointId = EndpointId.ENDPOINT_A1

        // Create a JSONObject with the desired structure
        val header = JSONObject().apply {
            put(
                Constants.INAPP_SUPPRESSED_META,
                JSONArray().put(mapOf(Constants.NOTIFICATION_ID_TAG to "112322222_646464646"))// key is not wzrk_id but value matches
            )
        }

        evaluationManager.suppressedClientSideInApps.add(mapOf("key" to "112322222_646464646"))

        // Act
        evaluationManager.onSentHeaders(header, endpointId)

        // Assert

        val resultList = evaluationManager.suppressedClientSideInApps
        assertEquals(1, resultList.size)
        assertEquals("112322222_646464646", resultList[0]["key"])
    }

    @Test
    fun `onSentHeaders should not crash when suppressedClientSideInApps have NOTIFICATION_ID_TAG mapped to other type`() {
        // Arrange
        val endpointId = EndpointId.ENDPOINT_A1

        // Create a JSONObject with the desired structure
        val header = JSONObject().apply {
            put(
                Constants.INAPP_SUPPRESSED_META,
                JSONArray().put(mapOf(Constants.NOTIFICATION_ID_TAG to "112322222_646464646"))// key is not wzrk_id but value matches
            )
        }

        evaluationManager.suppressedClientSideInApps.add(mapOf(Constants.NOTIFICATION_ID_TAG to JSONObject()))

        // Act
        evaluationManager.onSentHeaders(header, endpointId)

        // Assert

        val resultList = evaluationManager.suppressedClientSideInApps
        assertEquals(1, resultList.size)
    }

    @Test
    fun `onSentHeaders should not remove when evaluatedServerSideCampaignIds have id as 0`() {
        // Arrange
        val endpointId = EndpointId.ENDPOINT_A1

        // Create a JSONObject with the desired structure
        val header = JSONObject().apply {
            put(Constants.INAPP_SS_EVAL_META, JSONArray().put(0))
        }

        evaluationManager.evaluatedServerSideCampaignIds.add(1L)
        evaluationManager.evaluatedServerSideCampaignIds.add(2L)
        evaluationManager.evaluatedServerSideCampaignIds.add(3L)

        // Act
        evaluationManager.onSentHeaders(header, endpointId)

        // Assert

        val resultList = evaluationManager.evaluatedServerSideCampaignIds
        assertEquals(3, resultList.size)
    }

    @Test
    fun `test matchWhenLimitsBeforeDisplay`() {
        evaluationManager.matchWhenLimitsBeforeDisplay(emptyList(), "123")
        verify(exactly = 1) { limitsMatcher.matchWhenLimits(any(), any()) }
    }

    class FakeClock : Clock {

        override fun currentTimeMillis(): Long {
            return 10_000L
        }

        override fun newDate(): Date {
            val dateFormatter = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
            return dateFormatter.parse("20230126")!!// January 26, 2023
        }
    }
}