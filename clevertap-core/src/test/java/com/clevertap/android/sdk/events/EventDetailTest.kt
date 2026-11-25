package com.clevertap.android.sdk.events

import org.junit.Test
import kotlin.test.assertEquals

class EventDetailTest {

    @Test
    fun `getter functions and property syntax return the same values`() {
        val detail = EventDetail(3, 100, 200, "Charged")

        assertEquals(3, detail.getCount())
        assertEquals(3, detail.count)
        assertEquals(100, detail.getFirstTime())
        assertEquals(100, detail.firstTime)
        assertEquals(200, detail.getLastTime())
        assertEquals(200, detail.lastTime)
        assertEquals("Charged", detail.getName())
        assertEquals("Charged", detail.name)
    }
}
