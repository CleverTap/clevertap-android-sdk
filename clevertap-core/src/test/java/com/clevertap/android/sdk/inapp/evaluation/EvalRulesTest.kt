package com.clevertap.android.sdk.inapp.evaluation

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Shared whenTriggers/whenLimits parsing used by both the in-app and ND evaluators (SDK-6055).
 */
class EvalRulesTest {

    // ---- whenTriggers ----

    @Test
    fun `whenTriggers returns empty when array is empty`() {
        val json = JSONObject().put(Constants.INAPP_WHEN_TRIGGERS, JSONArray())
        assertEquals(emptyList(), EvalRules.whenTriggers(json))
    }

    @Test
    fun `whenTriggers returns empty when key is missing`() {
        assertEquals(emptyList(), EvalRules.whenTriggers(JSONObject()))
    }

    @Test
    fun `whenTriggers returns empty when value is not a JSONArray`() {
        val json = JSONObject().put(Constants.INAPP_WHEN_TRIGGERS, "not_an_array")
        assertEquals(emptyList(), EvalRules.whenTriggers(json))
    }

    @Test
    fun `whenTriggers skips non-JSONObject entries`() {
        val json = JSONObject().put(Constants.INAPP_WHEN_TRIGGERS, JSONArray().put("invalid"))
        assertEquals(emptyList(), EvalRules.whenTriggers(json))
    }

    @Test
    fun `whenTriggers parses each trigger`() {
        val t1 = JSONObject().put("eventName", "event1")
        val t2 = JSONObject().put("eventName", "event2")
        val json = JSONObject().put(Constants.INAPP_WHEN_TRIGGERS, JSONArray().put(t1).put(t2))

        val result = EvalRules.whenTriggers(json)

        assertEquals(2, result.size)
        assertEquals("event1", result[0].eventName)
        assertEquals("event2", result[1].eventName)
    }

    // ---- whenLimits ----

    @Test
    fun `whenLimits combines frequency and occurrence limits`() {
        val json = JSONObject().apply {
            put("frequencyLimits", JSONArray().put(JSONObject().apply { put("type", "minutes"); put("limit", 10); put("frequency", 30) }))
            put("occurrenceLimits", JSONArray().put(JSONObject().apply { put("type", "onExactly"); put("limit", 1) }))
        }

        val result = EvalRules.whenLimits(json)

        assertEquals(2, result.size)
        assertEquals(LimitType.Minutes, result[0].limitType)
        assertEquals(LimitType.OnExactly, result[1].limitType)
    }

    @Test
    fun `whenLimits returns empty for empty arrays`() {
        val json = JSONObject().apply {
            put("frequencyLimits", JSONArray())
            put("occurrenceLimits", JSONArray())
        }
        assertEquals(0, EvalRules.whenLimits(json).size)
    }

    @Test
    fun `whenLimits skips empty limit objects`() {
        val json = JSONObject().apply {
            put("frequencyLimits", JSONArray().put(JSONObject()))
            put("occurrenceLimits", JSONArray().put(JSONObject()))
        }
        assertEquals(0, EvalRules.whenLimits(json).size)
    }
}
