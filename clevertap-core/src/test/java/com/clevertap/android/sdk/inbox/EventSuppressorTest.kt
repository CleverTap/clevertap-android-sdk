package com.clevertap.android.sdk.inbox

import com.clevertap.android.sdk.TestClock
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventSuppressorTest {

    @Test
    fun `first call never suppressed`() {
        val s = EventSuppressor(windowMs = 2_000L, clock = TestClock(0L))
        assertFalse(s.shouldSuppress("m1"))
    }

    @Test
    fun `repeat inside window is suppressed`() {
        val clock = TestClock(0L)
        val s = EventSuppressor(windowMs = 2_000L, clock = clock)

        s.shouldSuppress("m1")
        clock.setCurrentTime(500L)

        assertTrue(s.shouldSuppress("m1"))
    }

    @Test
    fun `window slides on each suppressed call`() {
        val clock = TestClock(0L)
        val s = EventSuppressor(windowMs = 2_000L, clock = clock)

        s.shouldSuppress("m1")
        clock.setCurrentTime(1_500L)
        s.shouldSuppress("m1")
        clock.setCurrentTime(3_000L)

        // 3000 − 1500 = 1500 < 2000 → still suppressed
        assertTrue(s.shouldSuppress("m1"))
    }

    @Test
    fun `after enough silence the next call fires`() {
        val clock = TestClock(0L)
        val s = EventSuppressor(windowMs = 2_000L, clock = clock)

        s.shouldSuppress("m1")
        clock.setCurrentTime(2_500L)

        assertFalse(s.shouldSuppress("m1"))
    }

    @Test
    fun `different keys do not interfere`() {
        val s = EventSuppressor(windowMs = 2_000L, clock = TestClock(500L))

        assertFalse(s.shouldSuppress("m1"))
        assertFalse(s.shouldSuppress("m2"))
    }
}
