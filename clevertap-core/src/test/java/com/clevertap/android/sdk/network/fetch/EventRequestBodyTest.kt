package com.clevertap.android.sdk.network.fetch

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals

class EventRequestBodyTest {

    @Test
    fun `body is a 2-element JSONArray header then event`() {
        val header = JSONObject().put("g", "guid-xyz")
        val event = JSONObject()
            .put("type", "event")
            .put("evtName", "wzrk_fetch")

        val body = JSONArray(EventRequestBody(header, event).toJsonString())

        assertEquals(2, body.length())
        assertEquals("guid-xyz", body.getJSONObject(0).getString("g"))
        assertEquals("wzrk_fetch", body.getJSONObject(1).getString("evtName"))
    }

    @Test
    fun `different events produce different bodies while sharing the header`() {
        val header = JSONObject().put("g", "guid-xyz")
        val fetchEvent = JSONObject().put("evtName", "wzrk_fetch")
        val deleteEvent = JSONObject().put("evtName", "Message Deleted")

        val fetchBody = JSONArray(EventRequestBody(header, fetchEvent).toJsonString())
        val deleteBody = JSONArray(EventRequestBody(header, deleteEvent).toJsonString())

        assertEquals("wzrk_fetch", fetchBody.getJSONObject(1).getString("evtName"))
        assertEquals("Message Deleted", deleteBody.getJSONObject(1).getString("evtName"))
        assertEquals("guid-xyz", fetchBody.getJSONObject(0).getString("g"))
        assertEquals("guid-xyz", deleteBody.getJSONObject(0).getString("g"))
    }

    @Test
    fun `toJsonString is stable across calls`() {
        val body = EventRequestBody(
            JSONObject().put("g", "x"),
            JSONObject().put("evtName", "x")
        )
        assertEquals(body.toJsonString(), body.toJsonString())
    }
}
