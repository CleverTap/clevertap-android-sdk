package com.clevertap.android.sdk.network

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ContentFetchItemTest {

    @Test
    fun `from parses the self-describing fields and normalizes numeric tgtId to string`() {
        val raw = JSONObject()
            .put(Constants.CONTENT_FETCH_ITEM_EVENT_NAME, "App Launched")
            .put(Constants.CONTENT_FETCH_ITEM_RESPONSE_KEY, "inapp_notifs_applaunched")
            .put(Constants.CONTENT_FETCH_ITEM_TARGET_ID, 1787567474L)

        val item = ContentFetchItem.from(raw)

        assertEquals("App Launched", item.eventName)
        assertEquals("inapp_notifs_applaunched", item.responseKey)
        assertEquals("1787567474", item.targetId) // numeric tgtId -> String, comparable with ti
    }

    @Test
    fun `from tolerates missing fields as nulls`() {
        val item = ContentFetchItem.from(JSONObject())
        assertNull(item.eventName)
        assertNull(item.responseKey)
        assertNull(item.targetId)
    }

    @Test
    fun `listFrom skips non-object entries`() {
        val array = JSONArray()
            .put(JSONObject().put(Constants.CONTENT_FETCH_ITEM_TARGET_ID, "1"))
            .put("not-an-object")
            .put(JSONObject().put(Constants.CONTENT_FETCH_ITEM_TARGET_ID, "2"))

        val items = ContentFetchItem.listFrom(array)

        assertEquals(2, items.size)
        assertEquals(listOf("1", "2"), items.map { it.targetId })
    }

    @Test
    fun `syntheticInAppPayload is null without priority - keeps Option 2 dormant`() {
        val item = ContentFetchItem.from(
            JSONObject().put(Constants.CONTENT_FETCH_ITEM_TARGET_ID, "1787567474")
        )
        assertNull(item.syntheticInAppPayload())
    }

    @Test
    fun `syntheticInAppPayload carries only selection rules when priority is present`() {
        val whenTriggers = JSONArray().put(JSONObject().put("eventName", "App Launched"))
        val fcLimits = JSONArray().put(JSONObject().put("type", "session").put("limit", 1))
        val raw = JSONObject()
            .put(Constants.CONTENT_FETCH_ITEM_TARGET_ID, 1787567474L)
            .put(Constants.INAPP_PRIORITY, 90)
            .put(Constants.INAPP_WHEN_TRIGGERS, whenTriggers)
            .put(Constants.INAPP_FC_LIMITS, fcLimits)

        val payload = ContentFetchItem.from(raw).syntheticInAppPayload()!!

        assertEquals("1787567474", payload.optString(Constants.INAPP_ID_IN_PAYLOAD)) // ti <- tgtId
        assertEquals(90, payload.optInt(Constants.INAPP_PRIORITY))
        assertTrue(payload.has(Constants.INAPP_WHEN_TRIGGERS))
        assertTrue(payload.has(Constants.INAPP_FC_LIMITS))
        assertTrue(payload.optBoolean(Constants.INAPP_SYNTHETIC_CANDIDATE))
        // No display-time keys leak into the synthetic payload.
        assertTrue(!payload.has("efc") && !payload.has("mdc") && !payload.has("tdc"))
    }
}
