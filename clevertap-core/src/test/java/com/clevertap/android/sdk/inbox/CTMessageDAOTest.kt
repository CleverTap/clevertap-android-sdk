package com.clevertap.android.sdk.inbox

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CTMessageDAOTest {

    @Test
    fun `V1 JSON without isRead field parses as unread`() {
        val json = JSONObject("""{"_id":"m1","date":1,"wzrk_ttl":2,"msg":{}}""")
        val dao = CTMessageDAO.initWithJSON(json, "userA")
        assertNotNull(dao)
        assertEquals(0, dao!!.isRead)
    }

    @Test
    fun `V2 JSON with isRead=true parses as read`() {
        val json = JSONObject("""{"_id":"m1","isRead":true,"date":1,"wzrk_ttl":2,"msg":{}}""")
        val dao = CTMessageDAO.initWithJSON(json, "userA")
        assertNotNull(dao)
        assertEquals(1, dao!!.isRead)
    }

    @Test
    fun `V2 JSON with isRead=false parses as unread`() {
        val json = JSONObject("""{"_id":"m1","isRead":false,"date":1,"wzrk_ttl":2,"msg":{}}""")
        val dao = CTMessageDAO.initWithJSON(json, "userA")
        assertNotNull(dao)
        assertEquals(0, dao!!.isRead)
    }
}
