package com.clevertap.android.sdk

import org.junit.Test
import kotlin.test.assertEquals

class ConstantsTest {

    @Test
    fun `FETCH_TYPE_INBOX_V2 is 7`() {
        assertEquals(7, Constants.FETCH_TYPE_INBOX_V2)
    }

    @Test
    fun `INBOX_V2_JSON_RESPONSE_KEY matches server contract`() {
        assertEquals("inbox_notifs_v2", Constants.INBOX_V2_JSON_RESPONSE_KEY)
    }

    @Test
    fun `INBOX_V2_ISREAD_KEY matches server contract`() {
        assertEquals("isRead", Constants.INBOX_V2_ISREAD_KEY)
    }

    @Test
    fun `INBOX_V2_THROTTLE_WINDOW_MS is 5 minutes`() {
        assertEquals(5L * 60L * 1000L, Constants.INBOX_V2_THROTTLE_WINDOW_MS)
    }
}
