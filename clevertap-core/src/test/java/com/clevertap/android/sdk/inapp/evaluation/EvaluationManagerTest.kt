package com.clevertap.android.sdk.inapp.evaluation

import android.location.Location
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.inapp.customtemplates.TemplatesManager
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_DELAY_AFTER_TRIGGER
import com.clevertap.android.sdk.inapp.data.InAppInActionConstants
import com.clevertap.android.sdk.inapp.evaluation.TriggerAdapter.Companion.INAPP_OPERATOR
import com.clevertap.android.sdk.inapp.evaluation.TriggerAdapter.Companion.INAPP_PROPERTYNAME
import com.clevertap.android.sdk.inapp.evaluation.TriggerAdapter.Companion.KEY_PROPERTY_VALUE
import com.clevertap.android.sdk.inapp.evaluation.TriggerOperator.Equals
import com.clevertap.android.sdk.inapp.store.preference.InAppStore
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.network.EndpointId
import com.clevertap.android.sdk.toList
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
    private lateinit var templatesManager: TemplatesManager

    override fun setUp() {
        super.setUp()
        MockKAnnotations.init(this)
        triggersMatcher = mockk(relaxed = true)
        triggersManager = mockk(relaxed = true)
        limitsMatcher = mockk(relaxed = true)
        storeRegistry = mockk(relaxed = true)
        templatesManager = mockk(relaxed = true)
        evaluationManager = spyk(
            EvaluationManager(
                triggersMatcher = triggersMatcher,
                triggersManager = triggersManager,
                limitsMatcher = limitsMatcher,
                storeRegistry = storeRegistry,
                templatesManager = templatesManager
            )
        )
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
        // Mock both immediate and delayed client-side evaluations
        every { evaluationManager.evaluateClientSide(any()) } returns JSONArray().put(
            JSONObject(mapOf("resultKey" to "immediateValue"))
        ).toList()
        every { evaluationManager.evaluateDelayedClientSide(any()) } returns JSONArray().put(
            JSONObject(mapOf("delayKey" to "delayedValue"))
        ).toList()

        // Act
        val result = evaluationManager.evaluateOnEvent(eventName, eventProperties, userLocation)

        // Assert
        // Verify that the captured EventAdapter has the expected properties
        val capturedEventAdapter = eventAdapterSlot.captured[0]
        assertEquals(eventName, capturedEventAdapter.eventName)
        assertEquals(eventProperties, capturedEventAdapter.eventProperties)
        assertEquals(userLocation, capturedEventAdapter.userLocation)

        assertNotNull(result)

        // Verify immediate in-apps (first element of Pair)
        val immediateInApps = result.immediateClientSideInApps
        assertTrue(immediateInApps.isNotEmpty())
        val firstImmediateObject = immediateInApps[0]
        assertEquals("immediateValue", firstImmediateObject.getString("resultKey"))

        // Verify delayed in-apps (second element of Pair)
        val delayedInApps = result.delayedClientSideInApps
        assertTrue(delayedInApps.isNotEmpty())
        val firstDelayedObject = delayedInApps[0]
        assertEquals("delayedValue", firstDelayedObject.getString("delayKey"))

        // Verify method calls
        verify(exactly = 1) { evaluationManager.evaluateServerSide(any()) }
        verify(exactly = 1) { evaluationManager.evaluateClientSide(any()) }
        verify(exactly = 1) { evaluationManager.evaluateDelayedClientSide(any()) }
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
        ).toList()
        every { evaluationManager.evaluateDelayedClientSide(any()) } returns JSONArray().toList()

        // Act
        val result = evaluationManager.evaluateOnAppLaunchedClientSide(emptyMap(), userLocation)

        // Assert
        // Verify that the captured EventAdapter has the expected properties for app launched event
        val capturedEventAdapter = eventAdapterSlot.captured[0]
        assertEquals(Constants.APP_LAUNCHED_EVENT, capturedEventAdapter.eventName)
        assertEquals(emptyMap(), capturedEventAdapter.eventProperties)
        assertEquals(userLocation, capturedEventAdapter.userLocation)

        verify(exactly = 1) { evaluationManager.evaluateClientSide(any()) }
        verify(exactly = 1) { evaluationManager.evaluateDelayedClientSide(any()) }
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
        every { evaluationManager.evaluateClientSide(any()) } returns JSONArray().put(
            JSONObject(mapOf("resultKey" to "resultValue"))
        ).toList()
        every { evaluationManager.evaluateDelayedClientSide(any()) } returns JSONArray().toList()

        // Act
        val result = evaluationManager.evaluateOnChargedEvent(details, items, userLocation)

        // Assert
        // Verify that the captured EventAdapter has the expected event type
        val capturedEventAdapter = eventAdapterSlot.captured[0]
        assertEquals(Constants.CHARGED_EVENT, capturedEventAdapter.eventName)

        assertNotNull(result)

        // Access immediate in-apps from Pair
        val immediateInApps = result.immediateClientSideInApps
        assertTrue(immediateInApps.isNotEmpty())
        val firstResultObject = immediateInApps[0]
        assertEquals("resultValue", firstResultObject.getString("resultKey"))

        verify(exactly = 1) { evaluationManager.evaluateServerSide(any()) }
        verify(exactly = 1) { evaluationManager.evaluateClientSide(any()) }
        verify(exactly = 1) { evaluationManager.evaluateDelayedClientSide(any()) }
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
    fun `suppress should add entry to suppressedClientSideInApps with default values for raised event`() {
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
        assertTrue(evaluationManager.suppressedClientSideInApps.contains(expectedMap))
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
                      "$INAPP_PROPERTYNAME": "Property1",
                      "$INAPP_OPERATOR": 1,
                      "$KEY_PROPERTY_VALUE": "Value1"
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
    fun `test evaluateClientSide if inapp suppressed then does not return after evaluation for RAISED event`() {
        val inApp1 = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 60L)
            .put(Constants.INAPP_SUPPRESSED, true)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val inApps = listOf(inApp1)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(any(), any()) } returns inApps

        val evaluateClientSide = evaluationManager.evaluateClientSide(listOf(EventAdapter("", mapOf())))

        assertEquals(0, evaluateClientSide.size)
        assertEquals(1, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateClientSide when 1st inapp is suppressed while other not for RAISED event`() {
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

        assertEquals(1, evaluateClientSide.size)
        assertEquals(1, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateClientSide when both inapps are not suppressed for RAISED event`() {
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

        assertEquals(1, evaluateClientSide.size)
        assertEquals(0, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateClientSide if inapp not suppressed then does return after evaluation for RAISED event`() {
        val inApp1 = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 20L)
            .put(Constants.INAPP_SUPPRESSED, false)
        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val inApps = listOf(inApp1)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(any(), any()) } returns inApps

        val evaluateClientSide = evaluationManager.evaluateClientSide(listOf(EventAdapter("", mapOf())))

        assertEquals(1, evaluateClientSide.size)
        assertEquals(0, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateClientSide for multiple events if inapp suppressed then duplicate entries added to suppressed list for RAISED event`() {
        val inApp1 = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 60L)
            .put(Constants.INAPP_SUPPRESSED, true)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val inApps = listOf(inApp1)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(any(), any()) } returns inApps

        val evaluateClientSide =
            evaluationManager.evaluateClientSide(listOf(EventAdapter("", mapOf()), EventAdapter("", mapOf())))

        assertEquals(0, evaluateClientSide.size)
        assertEquals(2, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateClientSide for multiple events when 1st inapp is suppressed while other not then return after suppressing only one for RAISED event`() {
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

        val evaluateClientSide =
            evaluationManager.evaluateClientSide(listOf(EventAdapter("", mapOf()), EventAdapter("", mapOf())))

        assertEquals(1, evaluateClientSide.size)
        assertEquals(1, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateClientSide for multiple events when both inapps are not suppressed returns only one inapp for RAISED event`() {
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

        val evaluateClientSide =
            evaluationManager.evaluateClientSide(listOf(EventAdapter("", mapOf()), EventAdapter("", mapOf())))

        assertEquals(1, evaluateClientSide.size)
        assertEquals(0, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateClientSide for multiple events if inapp not suppressed and not elligible then does return after evaluation for RAISED event`() {
        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(any(), any()) } returns emptyList()

        val evaluateClientSide =
            evaluationManager.evaluateClientSide(listOf(EventAdapter("", mapOf()), EventAdapter("", mapOf())))

        assertEquals(0, evaluateClientSide.size)
        assertEquals(0, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateClientSide for multiple events if one triggers and the other doesn't then return inApps for RAISED event`() {
        val inApp1 = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 20L)
            .put(Constants.INAPP_SUPPRESSED, false)
        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val inApps = listOf(inApp1)
        val event1 = EventAdapter("event1", mapOf())
        val event2 = EventAdapter("event2", mapOf())
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(event1, any()) } returns inApps
        every { evaluationManager.evaluate(event2, any()) } returns emptyList()

        val evaluateClientSide = evaluationManager.evaluateClientSide(listOf(event1, event2))

        assertEquals(1, evaluateClientSide.size)
        assertEquals(0, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateClientSide for multiple events if both trigger but one is supressed for RAISED event`() {
        val inApp1 = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 20L)
            .put(Constants.INAPP_SUPPRESSED, false)

        val inAppSup = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 20L)
            .put(Constants.INAPP_SUPPRESSED, true)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val event1 = EventAdapter("event1", mapOf())
        val event2 = EventAdapter("event2", mapOf())
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(event1, any()) } returns listOf(inAppSup)
        every { evaluationManager.evaluate(event2, any()) } returns listOf(inApp1)

        val evaluateClientSide = evaluationManager.evaluateClientSide(listOf(event1, event2))

        assertEquals(1, evaluateClientSide.size)
        assertEquals(1, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateClientSide for multiple events if both trigger but one suppressed inapp has lower priority for RAISED event`() {
        val inApp1 = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 20L)
            .put(Constants.INAPP_SUPPRESSED, false)
            .put(Constants.INAPP_PRIORITY, 100)

        val inAppSup = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 20L)
            .put(Constants.INAPP_SUPPRESSED, true)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val event1 = EventAdapter("event1", mapOf())
        val event2 = EventAdapter("event2", mapOf())
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(event1, any()) } returns listOf(inAppSup)
        every { evaluationManager.evaluate(event2, any()) } returns listOf(inApp1)

        val evaluateClientSide = evaluationManager.evaluateClientSide(listOf(event1, event2))

        assertEquals(1, evaluateClientSide.size)
        assertEquals(0, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateClientSide for events with newValue and oldValue same`() {
        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val event1 =
            EventAdapter("event1", mapOf("oldValue" to "a", "newValue" to "a"), profileAttrName = "event1_CTChange")
        every { storeRegistry.inAppStore } returns mockInAppStore

        val evaluateClientSide = evaluationManager.evaluateClientSide(listOf(event1))

        assertEquals(0, evaluateClientSide.size)
        assertEquals(0, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateClientSide for events with newValue and oldValue different`() {
        val inApp1 = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 20L)
            .put(Constants.INAPP_SUPPRESSED, false)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val event1 =
            EventAdapter("event1", mapOf("oldValue" to "a", "newValue" to "b"), profileAttrName = "event1_CTChange")

        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(event1, any()) } returns listOf(inApp1)

        val evaluateClientSide = evaluationManager.evaluateClientSide(listOf(event1))

        assertEquals(1, evaluateClientSide.size)
    }

    @Test
    fun `test evaluateClientSide for events with newValue null and oldValue non-null`() {
        val inApp1 = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 20L)
            .put(Constants.INAPP_SUPPRESSED, false)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val event1 = EventAdapter("event1", mapOf("oldValue" to "a"), profileAttrName = "event1_CTChange")

        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(event1, any()) } returns listOf(inApp1)

        val evaluateClientSide = evaluationManager.evaluateClientSide(listOf(event1))

        assertEquals(1, evaluateClientSide.size)
    }

    @Test
    fun `test evaluateClientSide for events with newValue non-null and oldValue null`() {
        val inApp1 = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 20L)
            .put(Constants.INAPP_SUPPRESSED, false)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val event1 = EventAdapter("event1", mapOf("newValue" to "a"), profileAttrName = "event1_CTChange")

        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(event1, any()) } returns listOf(inApp1)

        val evaluateClientSide = evaluationManager.evaluateClientSide(listOf(event1))

        assertEquals(1, evaluateClientSide.size)
    }

    @Test
    fun `test evaluateClientSide for events with newValue and oldValue for suppressed inApp`() {
        val inApp1 = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 20L)
            .put(Constants.INAPP_SUPPRESSED, true)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val event1 = EventAdapter("event1", mapOf("newValue" to "a"), profileAttrName = "event1_CTChange")

        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(event1, any()) } returns listOf(inApp1)

        val evaluateClientSide = evaluationManager.evaluateClientSide(listOf(event1))

        assertEquals(0, evaluateClientSide.size)
        assertEquals(1, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateClientSide for events with newValue and oldValue for suppressed and unsuppressed inApp`() {
        val inApp1 = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 20L)
            .put(Constants.INAPP_SUPPRESSED, true)

        val inApp2 = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 20L)
            .put(Constants.INAPP_SUPPRESSED, false)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val event1 = EventAdapter("event1", mapOf("newValue" to "a"), profileAttrName = "event1_CTChange")

        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(event1, any()) } returns listOf(inApp1, inApp2)

        val evaluateClientSide = evaluationManager.evaluateClientSide(listOf(event1))

        assertEquals(1, evaluateClientSide.size)
        assertEquals(1, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateClientSide for events with newValue and oldValue for 2 unsuppressed inApps`() {
        val inApp1 = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 20L)
            .put(Constants.INAPP_SUPPRESSED, true)

        val inApp2 = JSONObject()
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 20L)
            .put(Constants.INAPP_SUPPRESSED, false)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val event1 = EventAdapter("event1", mapOf("newValue" to "a"), profileAttrName = "event1_CTChange")

        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(event1, any()) } returns listOf(inApp1, inApp2)

        val evaluateClientSide = evaluationManager.evaluateClientSide(listOf(event1))

        assertEquals(1, evaluateClientSide.size)
        assertEquals(1, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateOnAppLaunchedServerSide if inapp suppressed then does not return after evaluation`() {
        val inApp1 = JSONObject()
            .put(Constants.INAPP_SUPPRESSED, true)

        val inApps = listOf(inApp1)
        every { evaluationManager.evaluate(any(), any()) } returns inApps

        val evaluateClientSide = evaluationManager.evaluateOnAppLaunchedServerSide(inApps, emptyMap(), null)

        assertEquals(0, evaluateClientSide.size)
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

        assertEquals(1, evaluateClientSide.size)
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

        assertEquals(1, evaluateClientSide.size)
        assertEquals(0, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateOnAppLaunchedServerSide if inapp not suppressed then does return after evaluation`() {
        val inApp1 = JSONObject()
            .put(Constants.INAPP_SUPPRESSED, false)
        val inApps = listOf(inApp1)
        every { evaluationManager.evaluate(any(), any()) } returns inApps

        val evaluateClientSide = evaluationManager.evaluateOnAppLaunchedServerSide(inApps, emptyMap(), null)

        assertEquals(1, evaluateClientSide.size)
        assertEquals(0, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateServerSide when campaignId is 0 then don't add id to evaluatedServerSideCampaignIds for RAISED event`() {
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
    fun `test evaluateServerSide when campaignId is not 0 then add id to evaluatedServerSideCampaignIds for RAISED event`() {
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
    fun `test evaluateServerSide for multiple events when campaignId is 0 then don't add id to evaluatedServerSideCampaignIds for RAISED event`() {
        val inApp1 = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, 0)
        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val inApps = listOf(inApp1)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(any(), any()) } returns inApps

        evaluationManager.evaluateServerSide(listOf(EventAdapter("", mapOf()), EventAdapter("", mapOf())))

        assertEquals(0, evaluationManager.evaluatedServerSideCampaignIds.size)
        assertEquals(0, evaluationManager.evaluatedServerSideCampaignIds.size)
    }

    @Test
    fun `test evaluateServerSide for multiple inapps when campaignId is not 0 then add duplicate ids to evaluatedServerSideCampaignIds`() {
        val inApp1 = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, 123)
        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val inApps = listOf(inApp1)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(any(), any()) } returns inApps

        evaluationManager.evaluateServerSide(listOf(EventAdapter("", mapOf()), EventAdapter("", mapOf())))

        assertEquals(2, evaluationManager.evaluatedServerSideCampaignIds.size)
    }

    @Test
    fun `test evaluateServerSide for multiple inapps when campaignId is not 0 and only one event evaluates then add id to evaluatedServerSideCampaignIds for RAISED event`() {
        val inApp1 = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, 123)
        val mockInAppStore = mockk<InAppStore>(relaxed = true)

        val event1 = EventAdapter("event1", mapOf())
        val event2 = EventAdapter("event2", mapOf())
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(event1, any()) } returns listOf()
        every { evaluationManager.evaluate(event2, any()) } returns listOf(inApp1)

        evaluationManager.evaluateServerSide(listOf(event1, event2))

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
    fun `onAttachHeaders returns JSONObject when evaluatedServerSideCampaignIds is not empty for RAISED event`() {
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
    fun `onAttachHeaders returns JSONObject when evaluatedServerSideCampaignIds is not empty for PROFILE event`() {
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
    fun `onAttachHeaders returns JSONObject when suppressedClientSideInApps is not empty for RASIED event`() {
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
    fun `onAttachHeaders returns JSONObject when suppressedClientSideInApps is not empty for PROFILE event`() {
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
    fun `onAttachHeaders returns JSONObject when both lists are not empty for PROFILE event`() {
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
    fun `onAttachHeaders returns JSONObject when both lists are not empty for RAISED event`() {
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
    fun `onAttachHeaders returns null when both lists are empty for RAISED event`() {
        // Arrange
        val endpointId = EndpointId.ENDPOINT_A1

        // Act
        val result = evaluationManager.onAttachHeaders(endpointId)

        // Assert
        assertNull(result)
    }

    @Test
    fun `onAttachHeaders returns null when both lists are empty for PROFILE event`() {
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
    fun `onSentHeaders removes sent evaluatedServerSideCampaignIds and suppressedClientSideInApps for ENDPOINT_A1 for PROFILE event`() {
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
    fun `onSentHeaders removes sent evaluatedServerSideCampaignIds and suppressedClientSideInApps for ENDPOINT_A1 for RAISED event`() {
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
    fun `onSentHeaders doesn't remove sent suppressedClientSideInApps for ENDPOINT_A1 when NOTIFICATION_ID_TAG not present in header json for PROFILE event`() {
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
    fun `onSentHeaders doesn't remove sent suppressedClientSideInApps for ENDPOINT_A1 when NOTIFICATION_ID_TAG not present in header json for RAISED event`() {
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
    fun `onSentHeaders removes sent suppressedClientSideInApps for ENDPOINT_A1 when key in header json is different but id is same for PROFILE event`() {
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
    fun `onSentHeaders removes sent suppressedClientSideInApps for ENDPOINT_A1 when key in header json is different but id is same for RAISED event`() {
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
    fun `onSentHeaders should not remove when evaluatedServerSideCampaignIds have id as 0 for PROFILE event`() {
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

    @Test
    fun `test evaluateDelayedClientSide returns empty when store is null`() {
        // Arrange
        val event = EventAdapter("testEvent", emptyMap(), userLocation = null)
        every { storeRegistry.inAppStore } returns null

        // Act
        val result = evaluationManager.evaluateDelayedClientSide(listOf(event))

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun `test evaluateDelayedClientSide returns empty when no delayed in-apps in store`() {
        // Arrange
        val event = EventAdapter("testEvent", emptyMap(), userLocation = null)
        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { mockInAppStore.readClientSideDelayedInApps() } returns JSONArray().toList()

        // Act
        val result = evaluationManager.evaluateDelayedClientSide(listOf(event))

        // Assert
        assertEquals(0, result.size)
        verify(exactly = 1) { mockInAppStore.readClientSideDelayedInApps() }
    }

    @Test
    fun `test evaluateDelayedClientSide returns empty when evaluate returns empty list`() {
        // Arrange
        val event = EventAdapter("testEvent", emptyMap(), userLocation = null)
        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val delayedInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "delayed123")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)

        every { storeRegistry.inAppStore } returns mockInAppStore
        every { mockInAppStore.readClientSideDelayedInApps() } returns JSONArray().put(delayedInApp).toList()
        every { evaluationManager.evaluate(any(), any()) } returns emptyList()

        // Act
        val result = evaluationManager.evaluateDelayedClientSide(listOf(event))

        // Assert
        assertEquals(0, result.size)
        verify(exactly = 1) { evaluationManager.evaluate(event, listOf(delayedInApp)) }
    }

    @Test
    fun `test evaluateDelayedClientSide returns single in-app when not suppressed`() {
        // Arrange
        val event = EventAdapter("testEvent", emptyMap(), userLocation = null)
        val delayedInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "delayed123")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_SUPPRESSED, false)
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 60L)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { mockInAppStore.readClientSideDelayedInApps() } returns JSONArray().put(delayedInApp).toList()
        every { evaluationManager.evaluate(any(), any()) } returns listOf(delayedInApp)

        // Act
        val result = evaluationManager.evaluateDelayedClientSide(listOf(event))

        // Assert
        assertEquals(1, result.size)
        assertEquals("delayed123", result[0].getString(Constants.INAPP_ID_IN_PAYLOAD))
    }

    @Test
    fun `test evaluateDelayedClientSide groups by delay and returns one per delay group`() {
        // Arrange
        val event = EventAdapter("testEvent", emptyMap(), userLocation = null)

        val inApp10s_high = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "10s_high")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_SUPPRESSED, false)
            .put(Constants.INAPP_PRIORITY, 300)

        val inApp10s_low = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "10s_low")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_SUPPRESSED, false)
            .put(Constants.INAPP_PRIORITY, 100)

        val inApp20s = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "20s")
            .put(INAPP_DELAY_AFTER_TRIGGER, 20)
            .put(Constants.INAPP_SUPPRESSED, false)
            .put(Constants.INAPP_PRIORITY, 200)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val allInApps = listOf(inApp10s_high, inApp10s_low, inApp20s)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { mockInAppStore.readClientSideDelayedInApps() } returns JSONArray().apply {
            allInApps.forEach { put(it) }
        }.toList()
        every { evaluationManager.evaluate(any(), any()) } returns allInApps

        // Act
        val result = evaluationManager.evaluateDelayedClientSide(listOf(event))

        // Assert
        // Should return 2 in-apps: one from 10s group (highest priority) + one from 20s group
        assertEquals(2, result.size)

        val resultIds = (0 until result.size).map {
            result[it].getString(Constants.INAPP_ID_IN_PAYLOAD)
        }
        assertTrue(resultIds.contains("10s_high"))
        assertTrue(resultIds.contains("20s"))
        assertFalse(resultIds.contains("10s_low"))
    }

    @Test
    fun `test evaluateDelayedClientSide suppresses in-apps with INAPP_SUPPRESSED true`() {
        // Arrange
        val event = EventAdapter("testEvent", emptyMap(), userLocation = null)

        val suppressedInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "suppressed")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_SUPPRESSED, true)
            .put(Constants.INAPP_PRIORITY, 300)

        val normalInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "normal")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_SUPPRESSED, false)
            .put(Constants.INAPP_PRIORITY, 200)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val allInApps = listOf(suppressedInApp, normalInApp)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { mockInAppStore.readClientSideDelayedInApps() } returns JSONArray().apply {
            allInApps.forEach { put(it) }
        }.toList()
        every { evaluationManager.evaluate(any(), any()) } returns allInApps
        every { evaluationManager.generateWzrkId(any(), any()) } returns "suppressed_wzrk_id"

        // Act - clear any existing suppressed items first
        evaluationManager.suppressedClientSideInApps.clear()
        val result = evaluationManager.evaluateDelayedClientSide(listOf(event))

        // Assert
        assertEquals(1, result.size)
        assertEquals("normal", result[0].getString(Constants.INAPP_ID_IN_PAYLOAD))
        assertEquals(1, evaluationManager.suppressedClientSideInApps.size)
        verify(exactly = 1) { mockInAppStore.storeSuppressedClientSideInAppIds(any()) }
    }

    @Test
    fun `test evaluateDelayedClientSide returns empty when all in-apps suppressed`() {
        // Arrange
        val event = EventAdapter("testEvent", emptyMap(), userLocation = null)

        val suppressedInApp1 = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "suppressed1")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_SUPPRESSED, true)

        val suppressedInApp2 = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "suppressed2")
            .put(INAPP_DELAY_AFTER_TRIGGER, 20)
            .put(Constants.INAPP_SUPPRESSED, true)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val allInApps = listOf(suppressedInApp1, suppressedInApp2)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { mockInAppStore.readClientSideDelayedInApps() } returns JSONArray().apply {
            allInApps.forEach { put(it) }
        }.toList()
        every { evaluationManager.evaluate(any(), any()) } returns allInApps
        every { evaluationManager.generateWzrkId(any(), any()) } returns "wzrk_id"

        // Act
        evaluationManager.suppressedClientSideInApps.clear()
        val result = evaluationManager.evaluateDelayedClientSide(listOf(event))

        // Assert
        assertEquals(0, result.size)
        assertEquals(2, evaluationManager.suppressedClientSideInApps.size)
    }


    @Test
    fun `test evaluateDelayedClientSide handles profile event with same oldValue and newValue`() {
        // Arrange
        val event = EventAdapter(
            "attribute_CTChange",
            mapOf(Constants.KEY_OLD_VALUE to "same", Constants.KEY_NEW_VALUE to "same"),
            userLocation = null,
            profileAttrName = "attribute"
        )

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { mockInAppStore.readClientSideDelayedInApps() } returns JSONArray().toList()

        // Act
        val result = evaluationManager.evaluateDelayedClientSide(listOf(event))

        // Assert
        assertEquals(0, result.size)
        // Verify evaluate was NOT called because oldValue == newValue
        verify(exactly = 0) { evaluationManager.evaluate(any(), any()) }
    }

    @Test
    fun `test evaluateDelayedClientSide handles profile event with different oldValue and newValue`() {
        // Arrange
        val event = EventAdapter(
            "attribute_CTChange",
            mapOf(Constants.KEY_OLD_VALUE to "old", Constants.KEY_NEW_VALUE to "new"),
            userLocation = null,
            profileAttrName = "attribute"
        )

        val delayedInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "delayed123")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_SUPPRESSED, false)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { mockInAppStore.readClientSideDelayedInApps() } returns JSONArray().put(delayedInApp).toList()
        every { evaluationManager.evaluate(any(), any()) } returns listOf(delayedInApp)

        // Act
        val result = evaluationManager.evaluateDelayedClientSide(listOf(event))

        // Assert
        assertEquals(1, result.size)
        verify(exactly = 1) { evaluationManager.evaluate(event, listOf(delayedInApp)) }
    }

    @Test
    fun `test evaluateDelayedClientSide handles profile event with null newValue`() {
        // Arrange - newValue is null means attribute was deleted
        val event = EventAdapter(
            "attribute_CTChange",
            mapOf(Constants.KEY_OLD_VALUE to "old"),
            userLocation = null,
            profileAttrName = "attribute"
        )

        val delayedInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "delayed123")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_SUPPRESSED, false)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { mockInAppStore.readClientSideDelayedInApps() } returns JSONArray().put(delayedInApp).toList()
        every { evaluationManager.evaluate(any(), any()) } returns listOf(delayedInApp)

        // Act
        val result = evaluationManager.evaluateDelayedClientSide(listOf(event))

        // Assert
        assertEquals(1, result.size)
        verify(exactly = 1) { evaluationManager.evaluate(event, listOf(delayedInApp)) }
    }

    @Test
    fun `test evaluateDelayedClientSide handles multiple events and aggregates results`() {
        // Arrange
        val event1 = EventAdapter("event1", emptyMap(), userLocation = null)
        val event2 = EventAdapter("event2", emptyMap(), userLocation = null)

        val inApp1 = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "inapp1")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_SUPPRESSED, false)

        val inApp2 = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "inapp2")
            .put(INAPP_DELAY_AFTER_TRIGGER, 20)
            .put(Constants.INAPP_SUPPRESSED, false)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { mockInAppStore.readClientSideDelayedInApps() } returns JSONArray()
            .put(inApp1)
            .put(inApp2).toList()

        // event1 evaluates to inApp1, event2 evaluates to inApp2
        every { evaluationManager.evaluate(event1, any()) } returns listOf(inApp1)
        every { evaluationManager.evaluate(event2, any()) } returns listOf(inApp2)

        // Act
        val result = evaluationManager.evaluateDelayedClientSide(listOf(event1, event2))

        // Assert
        assertEquals(2, result.size)
        verify(exactly = 1) { evaluationManager.evaluate(event1, any()) }
        verify(exactly = 1) { evaluationManager.evaluate(event2, any()) }
    }

    @Test
    fun `test evaluateOnAppLaunchedDelayedServerSide returns empty when evaluate returns empty`() {
        // Arrange
        val delayedInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "delayed123")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)

        every { evaluationManager.evaluate(any(), any()) } returns emptyList()

        // Act
        val result = evaluationManager.evaluateOnAppLaunchedDelayedServerSide(
            listOf(delayedInApp),
            emptyMap(),
            null
        )

        // Assert
        assertEquals(0, result.size)
        verify(exactly = 1) { evaluationManager.evaluate(any(), listOf(delayedInApp)) }
    }

    @Test
    fun `test evaluateOnAppLaunchedDelayedServerSide returns single in-app when not suppressed`() {
        // Arrange
        val delayedInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "delayed123")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_SUPPRESSED, false)

        every { evaluationManager.evaluate(any(), any()) } returns listOf(delayedInApp)

        // Act
        val result = evaluationManager.evaluateOnAppLaunchedDelayedServerSide(
            listOf(delayedInApp),
            emptyMap(),
            null
        )

        // Assert
        assertEquals(1, result.size)
        assertEquals("delayed123", result[0].getString(Constants.INAPP_ID_IN_PAYLOAD))
    }

    @Test
    fun `test evaluateOnAppLaunchedDelayedServerSide groups by delay and selects one per group`() {
        // Arrange
        val inApp10s_high = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "10s_high")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_SUPPRESSED, false)
            .put(Constants.INAPP_PRIORITY, 300)

        val inApp10s_low = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "10s_low")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_SUPPRESSED, false)
            .put(Constants.INAPP_PRIORITY, 100)

        val inApp20s = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "20s")
            .put(INAPP_DELAY_AFTER_TRIGGER, 20)
            .put(Constants.INAPP_SUPPRESSED, false)
            .put(Constants.INAPP_PRIORITY, 200)

        val allInApps = listOf(inApp10s_high, inApp10s_low, inApp20s)
        every { evaluationManager.evaluate(any(), any()) } returns allInApps

        // Act
        val result = evaluationManager.evaluateOnAppLaunchedDelayedServerSide(
            allInApps,
            emptyMap(),
            null
        )

        // Assert
        assertEquals(2, result.size)
        val resultIds = (0 until result.size).map {
            result[it].getString(Constants.INAPP_ID_IN_PAYLOAD)
        }
        assertTrue(resultIds.contains("10s_high"))
        assertTrue(resultIds.contains("20s"))
        assertFalse(resultIds.contains("10s_low"))
    }

    @Test
    fun `test evaluateOnAppLaunchedDelayedServerSide suppresses and adds to suppression list`() {
        // Arrange
        val suppressedInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "suppressed")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_SUPPRESSED, true)
            .put(Constants.INAPP_PRIORITY, 300)

        val normalInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "normal")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_SUPPRESSED, false)
            .put(Constants.INAPP_PRIORITY, 200)

        val allInApps = listOf(suppressedInApp, normalInApp)
        every { evaluationManager.evaluate(any(), any()) } returns allInApps
        every { evaluationManager.generateWzrkId(any(), any()) } returns "suppressed_wzrk_id"

        // Act
        evaluationManager.suppressedClientSideInApps.clear()
        val result = evaluationManager.evaluateOnAppLaunchedDelayedServerSide(
            allInApps,
            emptyMap(),
            null
        )

        // Assert
        assertEquals(1, result.size)
        assertEquals("normal", result[0].getString(Constants.INAPP_ID_IN_PAYLOAD))
        assertTrue(evaluationManager.suppressedClientSideInApps.size > 0)
    }

    @Test
    fun `test evaluateOnAppLaunchedDelayedServerSide returns empty when all suppressed`() {
        // Arrange
        val suppressedInApp1 = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "suppressed1")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_SUPPRESSED, true)

        val suppressedInApp2 = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "suppressed2")
            .put(INAPP_DELAY_AFTER_TRIGGER, 20)
            .put(Constants.INAPP_SUPPRESSED, true)

        val allInApps = listOf(suppressedInApp1, suppressedInApp2)
        every { evaluationManager.evaluate(any(), any()) } returns allInApps
        every { evaluationManager.generateWzrkId(any(), any()) } returns "wzrk_id"

        // Act
        evaluationManager.suppressedClientSideInApps.clear()
        val result = evaluationManager.evaluateOnAppLaunchedDelayedServerSide(
            allInApps,
            emptyMap(),
            null
        )

        // Assert
        assertEquals(0, result.size)
        assertEquals(2, evaluationManager.suppressedClientSideInApps.size)
    }

    @Test
    fun `test evaluateOnAppLaunchedDelayedServerSide saves suppressed IDs when updated`() {
        // Arrange
        val suppressedInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "suppressed")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_SUPPRESSED, true)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { evaluationManager.evaluate(any(), any()) } returns listOf(suppressedInApp)
        every { evaluationManager.generateWzrkId(any(), any()) } returns "wzrk_id"

        // Act
        evaluationManager.evaluateOnAppLaunchedDelayedServerSide(
            listOf(suppressedInApp),
            emptyMap(),
            null
        )

        // Assert
        verify(exactly = 1) { mockInAppStore.storeSuppressedClientSideInAppIds(any()) }
    }

    @Test
    fun `test evaluateOnAppLaunchedDelayedServerSide creates correct EventAdapter`() {
        // Arrange
        val eventProperties = mapOf("key" to "value")
        val userLocation = mockk<Location>()
        val delayedInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "delayed123")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_SUPPRESSED, false)

        val eventAdapterSlot = slot<EventAdapter>()
        every { evaluationManager.evaluate(capture(eventAdapterSlot), any()) } returns listOf(delayedInApp)

        // Act
        evaluationManager.evaluateOnAppLaunchedDelayedServerSide(
            listOf(delayedInApp),
            eventProperties,
            userLocation
        )

        // Assert
        val capturedEventAdapter = eventAdapterSlot.captured
        assertEquals(Constants.APP_LAUNCHED_EVENT, capturedEventAdapter.eventName)
        assertEquals(eventProperties, capturedEventAdapter.eventProperties)
        assertEquals(userLocation, capturedEventAdapter.userLocation)
    }

    @Test
    fun `test evaluateOnAppLaunchedDelayedServerSide handles multiple delay groups correctly`() {
        // Arrange
        val inApp5s = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "5s")
            .put(INAPP_DELAY_AFTER_TRIGGER, 5)
            .put(Constants.INAPP_SUPPRESSED, false)

        val inApp10s = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "10s")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_SUPPRESSED, false)

        val inApp15s = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "15s")
            .put(INAPP_DELAY_AFTER_TRIGGER, 15)
            .put(Constants.INAPP_SUPPRESSED, false)

        val allInApps = listOf(inApp5s, inApp10s, inApp15s)
        every { evaluationManager.evaluate(any(), any()) } returns allInApps

        // Act
        val result = evaluationManager.evaluateOnAppLaunchedDelayedServerSide(
            allInApps,
            emptyMap(),
            null
        )

        // Assert - Should return 3 in-apps, one from each delay group
        assertEquals(3, result.size)
        val resultIds = (0 until result.size).map {
            result[it].getString(Constants.INAPP_ID_IN_PAYLOAD)
        }
        assertTrue(resultIds.contains("5s"))
        assertTrue(resultIds.contains("10s"))
        assertTrue(resultIds.contains("15s"))
    }

    @Test
    fun `test selectAndProcessEligibleInApps with Immediate strategy returns first non-suppressed in-app`() {
        // Arrange
        val strategy = InAppSelectionStrategy.Immediate

        val suppressedInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "suppressed")
            .put(Constants.INAPP_PRIORITY, 300)
            .put(Constants.INAPP_SUPPRESSED, true)

        val normalInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "normal")
            .put(Constants.INAPP_PRIORITY, 200)
            .put(Constants.INAPP_SUPPRESSED, false)
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 60L)

        val eligibleInApps = listOf(suppressedInApp, normalInApp)

        // Act
        val result = evaluationManager.selectAndProcessEligibleInApps(
            eligibleInApps,
            strategy,
            shouldUpdateTTLForThisContext = true
        )

        // Assert
        assertEquals(1, result.size)
        assertEquals("normal", result[0].getString(Constants.INAPP_ID_IN_PAYLOAD))
    }

    @Test
    fun `test selectAndProcessEligibleInApps with Delayed strategy returns one per delay group`() {
        // Arrange
        val strategy = InAppSelectionStrategy.Delayed

        // Delay group 10s
        val inApp10s_high = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "10s_high")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_PRIORITY, 300)
            .put(Constants.INAPP_SUPPRESSED, false)
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 60L)

        val inApp10s_low = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "10s_low")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_PRIORITY, 200)
            .put(Constants.INAPP_SUPPRESSED, false)
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 60L)

        // Delay group 20s
        val inApp20s = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "20s")
            .put(INAPP_DELAY_AFTER_TRIGGER, 20)
            .put(Constants.INAPP_PRIORITY, 100)
            .put(Constants.INAPP_SUPPRESSED, false)
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 60L)

        val eligibleInApps = listOf(inApp10s_high, inApp10s_low, inApp20s)

        // Act
        val result = evaluationManager.selectAndProcessEligibleInApps(
            eligibleInApps,
            strategy,
            shouldUpdateTTLForThisContext = true
        )

        // Assert - Should return 2 in-apps: highest priority from each delay group
        assertEquals(2, result.size)
        val resultIds = (0 until result.size).map {
            result[it].getString(Constants.INAPP_ID_IN_PAYLOAD)
        }
        assertTrue(resultIds.contains("10s_high"))
        assertTrue(resultIds.contains("20s"))
        assertFalse(resultIds.contains("10s_low"))
    }

    @Test
    fun `test selectAndProcessEligibleInApps calls sortByPriority before selection`() {
        // Arrange
        val strategy = InAppSelectionStrategy.Immediate

        val lowPriority = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "low")
            .put(Constants.INAPP_PRIORITY, 100)
            .put(Constants.INAPP_SUPPRESSED, false)

        val highPriority = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "high")
            .put(Constants.INAPP_PRIORITY, 300)
            .put(Constants.INAPP_SUPPRESSED, false)

        // Intentionally pass in wrong order
        val eligibleInApps = listOf(lowPriority, highPriority)
        every { evaluationManager.sortByPriority(any()) } answers { callOriginal() }

        // Act
        val result = evaluationManager.selectAndProcessEligibleInApps(
            eligibleInApps,
            strategy,
            shouldUpdateTTLForThisContext = true
        )

        // Assert - Should select high priority even though it was second in the list
        assertEquals(1, result.size)
        assertEquals("high", result[0].getString(Constants.INAPP_ID_IN_PAYLOAD))
        verify(exactly = 1) { evaluationManager.sortByPriority(eligibleInApps) }
    }

    @Test
    fun `test selectAndProcessEligibleInApps calls suppress for suppressed in-apps`() {
        // Arrange
        val strategy = InAppSelectionStrategy.Immediate

        val suppressedInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "suppressed")
            .put(Constants.INAPP_PRIORITY, 300)
            .put(Constants.INAPP_SUPPRESSED, true)

        val normalInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "normal")
            .put(Constants.INAPP_PRIORITY, 200)
            .put(Constants.INAPP_SUPPRESSED, false)

        val eligibleInApps = listOf(suppressedInApp, normalInApp)
        every { evaluationManager.suppress(any()) } just Runs

        // Act
        evaluationManager.selectAndProcessEligibleInApps(
            eligibleInApps,
            strategy,
            shouldUpdateTTLForThisContext = true
        )

        // Assert
        verify(exactly = 1) { evaluationManager.suppress(suppressedInApp) }
    }

    @Test
    fun `test selectAndProcessEligibleInApps updates TTL when shouldUpdateTTLForThisContext is true and strategy allows`() {
        // Arrange
        val strategy = InAppSelectionStrategy.Immediate // Immediate strategy should update TTL

        val inApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "inapp1")
            .put(Constants.INAPP_SUPPRESSED, false)
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 60L)

        val eligibleInApps = listOf(inApp)
        every { evaluationManager.updateTTL(any(), any()) } just Runs

        // Act
        evaluationManager.selectAndProcessEligibleInApps(
            eligibleInApps,
            strategy,
            shouldUpdateTTLForThisContext = true
        )

        // Assert
        verify(exactly = 1) { evaluationManager.updateTTL(inApp, any()) }
    }

    @Test
    fun `test selectAndProcessEligibleInApps does not update TTL when shouldUpdateTTLForThisContext is false`() {
        // Arrange
        val strategy = InAppSelectionStrategy.Immediate

        val inApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "inapp1")
            .put(Constants.INAPP_SUPPRESSED, false)

        val eligibleInApps = listOf(inApp)
        every { evaluationManager.updateTTL(any(), any()) } just Runs

        // Act
        evaluationManager.selectAndProcessEligibleInApps(
            eligibleInApps,
            strategy,
            shouldUpdateTTLForThisContext = false // SS context: no TTL update
        )

        // Assert
        verify(exactly = 0) { evaluationManager.updateTTL(any(), any()) }
    }

    @Test
    fun `test selectAndProcessEligibleInApps saves suppressed IDs when suppression occurs`() {
        // Arrange
        val strategy = InAppSelectionStrategy.Immediate

        val suppressedInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "suppressed")
            .put(Constants.INAPP_SUPPRESSED, true)
            .put(Constants.INAPP_PRIORITY, 100)

        val normalInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "normal")
            .put(Constants.INAPP_SUPPRESSED, false)
            .put(Constants.INAPP_PRIORITY, 99)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        every { storeRegistry.inAppStore } returns mockInAppStore
        val eligibleInApps = listOf(suppressedInApp, normalInApp)


        // Act
        evaluationManager.selectAndProcessEligibleInApps(
            eligibleInApps,
            strategy,
            shouldUpdateTTLForThisContext = true
        )

        // Assert
        verify(exactly = 1) { mockInAppStore.storeSuppressedClientSideInAppIds(any()) }
    }

    @Test
    fun `test selectAndProcessEligibleInApps does not save when no suppression occurs`() {
        // Arrange
        val strategy = InAppSelectionStrategy.Immediate

        val normalInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "normal")
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 60L)
            .put(Constants.INAPP_SUPPRESSED, false)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        val eligibleInApps = listOf(normalInApp)

        every { storeRegistry.inAppStore } returns mockInAppStore

        // Act
        evaluationManager.selectAndProcessEligibleInApps(
            eligibleInApps,
            strategy,
            shouldUpdateTTLForThisContext = true
        )

        // Assert
        verify(exactly = 0) { mockInAppStore.storeSuppressedClientSideInAppIds(any()) }
    }

    @Test
    fun `test selectAndProcessEligibleInApps returns empty when all in-apps are suppressed`() {
        // Arrange
        val strategy = InAppSelectionStrategy.Immediate

        val suppressed1 = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "suppressed1")
            .put(Constants.INAPP_SUPPRESSED, true)

        val suppressed2 = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "suppressed2")
            .put(Constants.INAPP_SUPPRESSED, true)

        val eligibleInApps = listOf(suppressed1, suppressed2)
        every { evaluationManager.generateWzrkId(any(), any()) } returns "wzrk_id"

        // Act
        evaluationManager.suppressedClientSideInApps.clear()
        val result = evaluationManager.selectAndProcessEligibleInApps(
            eligibleInApps,
            strategy,
            shouldUpdateTTLForThisContext = true
        )

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun `test selectAndProcessEligibleInApps with Delayed strategy handles multiple delay groups correctly`() {
        // Arrange
        val strategy = InAppSelectionStrategy.Delayed

        // Create 3 different delay groups
        val inApp5s = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "5s")
            .put(INAPP_DELAY_AFTER_TRIGGER, 5)
            .put(Constants.INAPP_PRIORITY, 100)
            .put(Constants.INAPP_SUPPRESSED, false)

        val inApp10s = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "10s")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_PRIORITY, 200)
            .put(Constants.INAPP_SUPPRESSED, false)

        val inApp15s = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "15s")
            .put(INAPP_DELAY_AFTER_TRIGGER, 15)
            .put(Constants.INAPP_PRIORITY, 300)
            .put(Constants.INAPP_SUPPRESSED, false)

        val eligibleInApps = listOf(inApp5s, inApp10s, inApp15s)

        // Act
        val result = evaluationManager.selectAndProcessEligibleInApps(
            eligibleInApps,
            strategy,
            shouldUpdateTTLForThisContext = true
        )

        // Assert - Should return all 3 (one per delay group)
        assertEquals(3, result.size)
    }

    @Test
    fun `test selectAndProcessEligibleInApps with Delayed strategy suppresses lower priority in same delay group`() {
        // Arrange
        val strategy = InAppSelectionStrategy.Delayed

        val inApp10s_suppressed = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "10s_suppressed")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_PRIORITY, 300)
            .put(Constants.INAPP_SUPPRESSED, true)

        val inApp10s_high = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "10s_high")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_PRIORITY, 200)
            .put(Constants.INAPP_SUPPRESSED, false)

        val inApp10s_low = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "10s_low")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_PRIORITY, 100)
            .put(Constants.INAPP_SUPPRESSED, false)

        val eligibleInApps = listOf(inApp10s_suppressed, inApp10s_high, inApp10s_low)
        every { evaluationManager.generateWzrkId(any(), any()) } returns "wzrk_id"

        // Act
        evaluationManager.suppressedClientSideInApps.clear()
        val result = evaluationManager.selectAndProcessEligibleInApps(
            eligibleInApps,
            strategy,
            shouldUpdateTTLForThisContext = true
        )

        // Assert
        assertEquals(1, result.size)
        assertEquals("10s_high", result[0].getString(Constants.INAPP_ID_IN_PAYLOAD))

        // Verify suppression occurred for the suppressed in-app
        assertTrue(evaluationManager.suppressedClientSideInApps.size > 0)
    }

    @Test
    fun `test selectAndProcessEligibleInApps builds JSONArray in correct order`() {
        // Arrange
        val strategy = InAppSelectionStrategy.Delayed

        val inApp10s = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "10s")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_SUPPRESSED, false)

        val inApp20s = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "20s")
            .put(INAPP_DELAY_AFTER_TRIGGER, 20)
            .put(Constants.INAPP_SUPPRESSED, false)

        val eligibleInApps = listOf(inApp10s, inApp20s)

        // Act
        val result = evaluationManager.selectAndProcessEligibleInApps(
            eligibleInApps,
            strategy,
            shouldUpdateTTLForThisContext = true
        )

        // Assert
        assertEquals(2, result.size)
        // Verify order is maintained
        assertEquals("10s", result[0].getString(Constants.INAPP_ID_IN_PAYLOAD))
        assertEquals("20s", result[1].getString(Constants.INAPP_ID_IN_PAYLOAD))
    }

    @Test
    fun `test selectAndProcessEligibleInApps with Delayed strategy does not update TTL for each selected in-app`() {
        // Arrange
        val strategy = InAppSelectionStrategy.Delayed

        val inApp10s = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "10s")
            .put(INAPP_DELAY_AFTER_TRIGGER, 10)
            .put(Constants.INAPP_SUPPRESSED, false)
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 60L)

        val inApp20s = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "20s")
            .put(INAPP_DELAY_AFTER_TRIGGER, 20)
            .put(Constants.INAPP_SUPPRESSED, false)
            .put(Constants.WZRK_TIME_TO_LIVE_OFFSET, 60L)

        val eligibleInApps = listOf(inApp10s, inApp20s)
        every { evaluationManager.updateTTL(any(), any()) } just Runs

        // Act
        evaluationManager.selectAndProcessEligibleInApps(
            eligibleInApps,
            strategy,
            shouldUpdateTTLForThisContext = true
        )

        // Assert - TTL should not be updated for both selected in-apps
        verify(exactly = 0) { evaluationManager.updateTTL(inApp10s, any()) }
        verify(exactly = 0) { evaluationManager.updateTTL(inApp20s, any()) }
    }

    @Test
    fun `test selectAndProcessEligibleInApps with Immediate strategy stops after first non-suppressed`() {
        // Arrange
        val strategy = InAppSelectionStrategy.Immediate

        val inApp1 = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "inapp1")
            .put(Constants.INAPP_PRIORITY, 300)
            .put(Constants.INAPP_SUPPRESSED, false)

        val inApp2 = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "inapp2")
            .put(Constants.INAPP_PRIORITY, 200)
            .put(Constants.INAPP_SUPPRESSED, false)

        val inApp3 = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, "inapp3")
            .put(Constants.INAPP_PRIORITY, 100)
            .put(Constants.INAPP_SUPPRESSED, false)

        val eligibleInApps = listOf(inApp1, inApp2, inApp3)
        every { evaluationManager.updateTTL(any(), any()) } just Runs

        // Act
        val result = evaluationManager.selectAndProcessEligibleInApps(
            eligibleInApps,
            strategy,
            shouldUpdateTTLForThisContext = true
        )

        // Assert - Should return only first one
        assertEquals(1, result.size)
        assertEquals("inapp1", result[0].getString(Constants.INAPP_ID_IN_PAYLOAD))

        // Should only update TTL for the selected one
        verify(exactly = 1) { evaluationManager.updateTTL(inApp1, any()) }
        verify(exactly = 0) { evaluationManager.updateTTL(inApp2, any()) }
        verify(exactly = 0) { evaluationManager.updateTTL(inApp3, any()) }
    }
    // ==================== EVALUATE SERVER SIDE IN-ACTION TESTS ====================

    @Test
    fun `test evaluateServerSideInAction returns empty list when inAppStore is null`() {
        // Arrange
        every { storeRegistry.inAppStore } returns null
        val events = listOf(EventAdapter("testEvent", emptyMap(), userLocation = null))

        // Act
        val result = evaluationManager.evaluateServerSideInAction(events)

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun `test evaluateServerSideInAction returns empty list when no metadata exists`() {
        // Arrange
        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { mockInAppStore.readServerSideInActionMetaData() } returns emptyList()

        val events = listOf(EventAdapter("testEvent", emptyMap(), userLocation = null))

        // Act
        val result = evaluationManager.evaluateServerSideInAction(events)

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun `test evaluateServerSideInAction returns eligible in-apps when event matches`() {
        // Arrange
        val inActionInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, 12345L)
            .put(InAppInActionConstants.INAPP_INACTION_DURATION, 60)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { mockInAppStore.readServerSideInActionMetaData() } returns listOf(inActionInApp)
        every { evaluationManager.evaluate(any(), any()) } returns listOf(inActionInApp)

        val event = EventAdapter("testEvent", emptyMap(), userLocation = null)

        // Act
        val result = evaluationManager.evaluateServerSideInAction(listOf(event))

        // Assert
        assertEquals(1, result.size)
        assertEquals(12345L, result[0].optLong(Constants.INAPP_ID_IN_PAYLOAD))
    }

    @Test
    fun `test evaluateServerSideInAction tracks evaluated campaign IDs`() {
        // Arrange
        val inActionInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, 12345L)
            .put(InAppInActionConstants.INAPP_INACTION_DURATION, 60)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { mockInAppStore.readServerSideInActionMetaData() } returns listOf(inActionInApp)
        every { evaluationManager.evaluate(any(), any()) } returns listOf(inActionInApp)

        val event = EventAdapter("testEvent", emptyMap(), userLocation = null)
        evaluationManager.evaluatedServerSideCampaignIds.clear()

        // Act
        evaluationManager.evaluateServerSideInAction(listOf(event))

        // Assert
        assertEquals(1, evaluationManager.evaluatedServerSideCampaignIds.size)
        assertEquals(12345L, evaluationManager.evaluatedServerSideCampaignIds[0])
    }

    @Test
    fun `test evaluateServerSideInAction does not track campaign ID when it is 0`() {
        // Arrange
        val inActionInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, 0L)
            .put(InAppInActionConstants.INAPP_INACTION_DURATION, 60)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { mockInAppStore.readServerSideInActionMetaData() } returns listOf(inActionInApp)
        every { evaluationManager.evaluate(any(), any()) } returns listOf(inActionInApp)

        val event = EventAdapter("testEvent", emptyMap(), userLocation = null)
        evaluationManager.evaluatedServerSideCampaignIds.clear()

        // Act
        evaluationManager.evaluateServerSideInAction(listOf(event))

        // Assert
        assertEquals(0, evaluationManager.evaluatedServerSideCampaignIds.size)
    }

    @Test
    fun `test evaluateServerSideInAction handles multiple events`() {
        // Arrange
        val inActionInApp1 = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, 111L)
            .put(InAppInActionConstants.INAPP_INACTION_DURATION, 60)

        val inActionInApp2 = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, 222L)
            .put(InAppInActionConstants.INAPP_INACTION_DURATION, 120)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { mockInAppStore.readServerSideInActionMetaData() } returns listOf(inActionInApp1, inActionInApp2)

        val event1 = EventAdapter("event1", emptyMap(), userLocation = null)
        val event2 = EventAdapter("event2", emptyMap(), userLocation = null)

        every { evaluationManager.evaluate(event1, any()) } returns listOf(inActionInApp1)
        every { evaluationManager.evaluate(event2, any()) } returns listOf(inActionInApp2)

        // Act
        val result = evaluationManager.evaluateServerSideInAction(listOf(event1, event2))

        // Assert
        assertEquals(2, result.size)
    }

    @Test
    fun `test evaluateServerSideInAction returns empty when no events match`() {
        // Arrange
        val inActionInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, 12345L)
            .put(InAppInActionConstants.INAPP_INACTION_DURATION, 60)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { mockInAppStore.readServerSideInActionMetaData() } returns listOf(inActionInApp)
        every { evaluationManager.evaluate(any(), any()) } returns emptyList()

        val event = EventAdapter("nonMatchingEvent", emptyMap(), userLocation = null)

        // Act
        val result = evaluationManager.evaluateServerSideInAction(listOf(event))

        // Assert
        assertEquals(0, result.size)
    }

    @Test
    fun `test evaluateOnEvent returns Triple with in-action in-apps`() {
        // Arrange
        val eventName = "customEvent"
        val eventProperties = mapOf("key" to "value")
        val userLocation = mockk<Location>()

        val immediateInApp = JSONObject().put("type", "immediate")
        val delayedInApp = JSONObject().put("type", "delayed")
        val inActionInApp = JSONObject().put("type", "inaction")

        val eventAdapterSlot = slot<List<EventAdapter>>()
        every { evaluationManager.evaluateServerSide(capture(eventAdapterSlot)) } returns Unit
        every { evaluationManager.evaluateClientSide(any()) } returns listOf(immediateInApp)
        every { evaluationManager.evaluateDelayedClientSide(any()) } returns listOf(delayedInApp)
        every { evaluationManager.evaluateServerSideInAction(any()) } returns listOf(inActionInApp)

        // Act
        val result = evaluationManager.evaluateOnEvent(eventName, eventProperties, userLocation)

        // Assert
        assertNotNull(result)

        // Verify immediate in-apps (first)
        assertEquals(1, result.immediateClientSideInApps.size)
        assertEquals("immediate", result.immediateClientSideInApps[0].getString("type"))

        // Verify delayed in-apps (second)
        assertEquals(1, result.delayedClientSideInApps.size)
        assertEquals("delayed", result.delayedClientSideInApps[0].getString("type"))

        // Verify in-action in-apps (third)
        assertEquals(1, result.serverSideInActionInApps.size)
        assertEquals("inaction", result.serverSideInActionInApps[0].getString("type"))

        verify(exactly = 1) { evaluationManager.evaluateServerSide(any()) }
        verify(exactly = 1) { evaluationManager.evaluateClientSide(any()) }
        verify(exactly = 1) { evaluationManager.evaluateDelayedClientSide(any()) }
        verify(exactly = 1) { evaluationManager.evaluateServerSideInAction(any()) }
    }

    @Test
    fun `test evaluateOnChargedEvent returns Triple with in-action in-apps`() {
        // Arrange
        val details = mapOf("key" to "value")
        val items = listOf(mapOf("itemKey" to "itemValue"))
        val userLocation = mockk<Location>()

        val immediateInApp = JSONObject().put("type", "immediate")
        val delayedInApp = JSONObject().put("type", "delayed")
        val inActionInApp = JSONObject().put("type", "inaction")

        val eventAdapterSlot = slot<List<EventAdapter>>()
        every { evaluationManager.evaluateServerSide(capture(eventAdapterSlot)) } returns Unit
        every { evaluationManager.evaluateClientSide(any()) } returns listOf(immediateInApp)
        every { evaluationManager.evaluateDelayedClientSide(any()) } returns listOf(delayedInApp)
        every { evaluationManager.evaluateServerSideInAction(any()) } returns listOf(inActionInApp)

        // Act
        val result = evaluationManager.evaluateOnChargedEvent(details, items, userLocation)

        // Assert
        val capturedEventAdapter = eventAdapterSlot.captured[0]
        assertEquals(Constants.CHARGED_EVENT, capturedEventAdapter.eventName)

        // Verify Triple elements
        assertEquals(1, result.immediateClientSideInApps.size)
        assertEquals(1, result.delayedClientSideInApps.size)
        assertEquals(1, result.serverSideInActionInApps.size)
        assertEquals("inaction", result.serverSideInActionInApps[0].getString("type"))

        verify(exactly = 1) { evaluationManager.evaluateServerSideInAction(any()) }
    }

    @Test
    fun `test evaluateOnUserAttributeChange returns Triple with in-action in-apps`() {
        // Arrange
        val eventProperties = mapOf(
            "attribute1" to mapOf("oldValue" to "old", "newValue" to "new")
        )
        val userLocation = mockk<Location>()
        val appFields = mapOf("appField" to "value")

        val immediateInApp = JSONObject().put("type", "immediate")
        val delayedInApp = JSONObject().put("type", "delayed")
        val inActionInApp = JSONObject().put("type", "inaction")

        every { evaluationManager.evaluateServerSide(any()) } returns Unit
        every { evaluationManager.evaluateClientSide(any()) } returns listOf(immediateInApp)
        every { evaluationManager.evaluateDelayedClientSide(any()) } returns listOf(delayedInApp)
        every { evaluationManager.evaluateServerSideInAction(any()) } returns listOf(inActionInApp)

        // Act
        val result = evaluationManager.evaluateOnUserAttributeChange(eventProperties, userLocation, appFields)

        // Assert
        assertEquals(1, result.immediateClientSideInApps.size)
        assertEquals(1, result.delayedClientSideInApps.size)
        assertEquals(1, result.serverSideInActionInApps.size)
        assertEquals("inaction", result.serverSideInActionInApps[0].getString("type"))

        verify(exactly = 1) { evaluationManager.evaluateServerSideInAction(any()) }
    }

    @Test
    fun `test evaluateOnEvent returns empty in-action list when no in-action metadata exists`() {
        // Arrange
        val eventName = "customEvent"
        val eventProperties = mapOf("key" to "value")

        every { evaluationManager.evaluateServerSide(any()) } returns Unit
        every { evaluationManager.evaluateClientSide(any()) } returns emptyList()
        every { evaluationManager.evaluateDelayedClientSide(any()) } returns emptyList()
        every { evaluationManager.evaluateServerSideInAction(any()) } returns emptyList()

        // Act
        val result = evaluationManager.evaluateOnEvent(eventName, eventProperties, null)

        // Assert
        assertTrue(result.immediateClientSideInApps.isEmpty())
        assertTrue(result.delayedClientSideInApps.isEmpty())
        assertTrue(result.serverSideInActionInApps.isEmpty())
    }

    @Test
    fun `test evaluateServerSideInAction adds duplicate IDs for multiple matching events`() {
        // Arrange
        val inActionInApp = JSONObject()
            .put(Constants.INAPP_ID_IN_PAYLOAD, 12345L)
            .put(InAppInActionConstants.INAPP_INACTION_DURATION, 60)

        val mockInAppStore = mockk<InAppStore>(relaxed = true)
        every { storeRegistry.inAppStore } returns mockInAppStore
        every { mockInAppStore.readServerSideInActionMetaData() } returns listOf(inActionInApp)
        every { evaluationManager.evaluate(any(), any()) } returns listOf(inActionInApp)

        val event1 = EventAdapter("event1", emptyMap(), userLocation = null)
        val event2 = EventAdapter("event2", emptyMap(), userLocation = null)
        evaluationManager.evaluatedServerSideCampaignIds.clear()

        // Act
        evaluationManager.evaluateServerSideInAction(listOf(event1, event2))

        // Assert - Both events matched the same in-app, so ID is added twice
        assertEquals(2, evaluationManager.evaluatedServerSideCampaignIds.size)
        assertEquals(12345L, evaluationManager.evaluatedServerSideCampaignIds[0])
        assertEquals(12345L, evaluationManager.evaluatedServerSideCampaignIds[1])
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