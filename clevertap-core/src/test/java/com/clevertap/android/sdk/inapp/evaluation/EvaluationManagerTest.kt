package com.clevertap.android.sdk.inapp.evaluation

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
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