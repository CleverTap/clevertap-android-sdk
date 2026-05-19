package com.clevertap.android.sdk.network.fetch

import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.utils.Clock
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InboxV2EventBuilderTest {

    private val coreMetaData = mockk<CoreMetaData>(relaxed = true)
    private val fixedClock = object : Clock {
        override fun currentTimeMillis(): Long = 1_700_000_000_000L
        override fun newDate(): java.util.Date = java.util.Date(currentTimeMillis())
    }

    @Before
    fun resetActivityCount() {
        CoreMetaData.setActivityCount(3)
    }

    @After
    fun cleanup() {
        CoreMetaData.setActivityCount(0)
    }

    @Test
    fun `stamps full metadata when screenName is set`() {
        every { coreMetaData.currentSessionId } returns 42
        every { coreMetaData.isFirstSession } returns true
        every { coreMetaData.lastSessionLength } returns 120
        every { coreMetaData.screenName } returns "Home"

        val event = buildInboxV2Event(
            evtName = "wzrk_fetch",
            evtData = JSONObject().put("t", 7),
            coreMetaData = coreMetaData,
            clock = fixedClock,
            packageName = "com.example.app"
        )

        assertEquals("event", event.getString("type"))
        assertEquals("wzrk_fetch", event.getString("evtName"))
        assertEquals(7, event.getJSONObject("evtData").getInt("t"))
        assertEquals(42, event.getInt("s"))
        assertEquals(3, event.getInt("pg"))
        assertEquals(1_700_000_000, event.getInt("ep"))
        assertTrue(event.getBoolean("f"))
        assertEquals(120, event.getInt("lsl"))
        assertEquals("com.example.app", event.getString("pai"))
        assertEquals("Home", event.getString("n"))
    }

    @Test
    fun `omits n when screenName is null`() {
        every { coreMetaData.currentSessionId } returns 1
        every { coreMetaData.isFirstSession } returns false
        every { coreMetaData.lastSessionLength } returns 0
        every { coreMetaData.screenName } returns null

        val event = buildInboxV2Event(
            evtName = "Message Deleted",
            evtData = JSONObject(),
            coreMetaData = coreMetaData,
            clock = fixedClock,
            packageName = "com.example.app"
        )

        assertFalse(event.has("n"))
        assertEquals("Message Deleted", event.getString("evtName"))
        assertEquals("com.example.app", event.getString("pai"))
    }
}
