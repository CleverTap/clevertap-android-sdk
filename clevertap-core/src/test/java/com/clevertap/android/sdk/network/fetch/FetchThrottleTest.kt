package com.clevertap.android.sdk.network.fetch

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.TestClock
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FetchThrottleTest {

    private fun inboxThrottle(clock: TestClock = TestClock(60L * 60 * 1000)) =
        FetchThrottle(
            windowMs = Constants.INBOX_V2_THROTTLE_WINDOW_MS,
            clock = clock
        )

    @Test
    fun `first call is never throttled`() {
        assertFalse(inboxThrottle().shouldThrottle())
    }

    @Test
    fun `after recordFetch, shouldThrottle is true within window`() {
        val clock = TestClock(60L * 60 * 1000)
        val t = inboxThrottle(clock)
        t.recordFetch()
        clock.advanceTime(2L * 60 * 1000)
        assertTrue(t.shouldThrottle())
    }

    @Test
    fun `after windowMs elapsed, shouldThrottle is false`() {
        val clock = TestClock(60L * 60 * 1000)
        val t = inboxThrottle(clock)
        t.recordFetch()
        clock.advanceTime(6L * 60 * 1000)
        assertFalse(t.shouldThrottle())
    }

    @Test
    fun `different instances have independent state`() {
        val clock = TestClock(60L * 60 * 1000)
        val a = inboxThrottle(clock)
        val b = inboxThrottle(clock)

        a.recordFetch()

        assertTrue(a.shouldThrottle())
        assertFalse(b.shouldThrottle())
    }

    @Test
    fun `two features with different windows do not collide`() {
        val clock = TestClock(60L * 60 * 1000)
        val inbox = FetchThrottle(windowMs = 5L * 60_000, clock = clock)
        val recs = FetchThrottle(windowMs = 10L * 60_000, clock = clock)

        inbox.recordFetch()

        assertTrue(inbox.shouldThrottle())
        assertFalse(recs.shouldThrottle())
    }
}
