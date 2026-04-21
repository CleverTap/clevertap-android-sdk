package com.clevertap.android.sdk.inbox

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class CTMessageDAOTest {

    @Test
    fun `V1 JSON without isRead field parses as unread`() {
        val json = JSONObject("""{"_id":"m1","date":1,"wzrk_ttl":2,"msg":{}}""")
        val dao = CTMessageDAO.initWithJSON(json, "userA", InboxMessageSource.V1)
        assertNotNull(dao)
        assertEquals(0, dao!!.isRead)
    }

    @Test
    fun `V2 JSON with isRead=true parses as read`() {
        val json = JSONObject("""{"_id":"m1","isRead":true,"date":1,"wzrk_ttl":2,"msg":{}}""")
        val dao = CTMessageDAO.initWithJSON(json, "userA", InboxMessageSource.V2)
        assertNotNull(dao)
        assertEquals(1, dao!!.isRead)
    }

    @Test
    fun `V2 JSON with isRead=false parses as unread`() {
        val json = JSONObject("""{"_id":"m1","isRead":false,"date":1,"wzrk_ttl":2,"msg":{}}""")
        val dao = CTMessageDAO.initWithJSON(json, "userA", InboxMessageSource.V2)
        assertNotNull(dao)
        assertEquals(0, dao!!.isRead)
    }

    @Test
    fun `initWithJSON tags V1 source when requested`() {
        val json = JSONObject("""{"_id":"m1","date":1,"wzrk_ttl":2,"msg":{}}""")
        val dao = CTMessageDAO.initWithJSON(json, "userA", InboxMessageSource.V1)
        assertEquals(InboxMessageSource.V1, dao!!.source)
    }

    @Test
    fun `initWithJSON tags V2 source when requested`() {
        val json = JSONObject("""{"_id":"m1","isRead":true,"date":1,"wzrk_ttl":2,"msg":{}}""")
        val dao = CTMessageDAO.initWithJSON(json, "userA", InboxMessageSource.V2)
        assertEquals(InboxMessageSource.V2, dao!!.source)
    }

    @Test
    fun `toJSON does NOT leak source — customer-observable JSON stays clean`() {
        val json = JSONObject("""{"_id":"m1","date":1,"wzrk_ttl":2,"msg":{}}""")
        val v2 = CTMessageDAO.initWithJSON(json, "userA", InboxMessageSource.V2)!!

        val asJson = v2.toJSON()

        assertFalse(asJson.has("source"))
        assertFalse(asJson.has("Source"))
        assertFalse(asJson.has("__source"))
        assertFalse(asJson.has("__src"))

        // The public CTInboxMessage wraps this JSON and exposes it via
        // getData() — the raw JSON must not carry the V1/V2 tag either.
        val publicMsg = CTInboxMessage(asJson)
        assertFalse(publicMsg.data.has("source"))
    }
}
