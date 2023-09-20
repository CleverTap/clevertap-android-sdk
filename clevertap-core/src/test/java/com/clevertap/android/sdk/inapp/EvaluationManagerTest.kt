package com.clevertap.android.sdk.inapp

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.shared.test.BaseTestCase
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.json.JSONObject
import org.junit.Test

class EvaluationManagerTest : BaseTestCase() {

    private lateinit var evaluationManager: EvaluationManager

    override fun setUp() {
        super.setUp()
        evaluationManager = EvaluationManager()
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

}