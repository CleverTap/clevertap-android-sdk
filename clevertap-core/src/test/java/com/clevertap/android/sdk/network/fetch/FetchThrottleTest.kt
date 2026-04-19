package com.clevertap.android.sdk.network.fetch

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.TestClock
import com.clevertap.android.sdk.utils.Clock
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class FetchThrottleTest {

    private fun ctx(): Context = ApplicationProvider.getApplicationContext()

    private fun configFor(accountId: String): CleverTapInstanceConfig =
        CleverTapInstanceConfig.createInstance(ctx(), accountId, "token")

    private fun inboxThrottle(
        accountId: String,
        clock: Clock = Clock.SYSTEM
    ) = FetchThrottle(
        ctx(),
        configFor(accountId),
        prefKey = Constants.INBOX_V2_LAST_FETCH_TS_KEY,
        windowMs = Constants.INBOX_V2_THROTTLE_WINDOW_MS,
        clock = clock
    )

    @Test
    fun `first call is never throttled`() {
        assertFalse(inboxThrottle("acct-first").shouldThrottle())
    }

    @Test
    fun `after recordFetch, shouldThrottle is true within window`() {
        val clock = TestClock(60L * 60 * 1000)
        val t = inboxThrottle("acct-within", clock)
        t.recordFetch()
        clock.advanceTime(2L * 60 * 1000)
        assertTrue(t.shouldThrottle())
    }

    @Test
    fun `after windowMs elapsed, shouldThrottle is false`() {
        val clock = TestClock(60L * 60 * 1000)
        val t = inboxThrottle("acct-elapsed", clock)
        t.recordFetch()
        clock.advanceTime(6L * 60 * 1000)
        assertFalse(t.shouldThrottle())
    }

    @Test
    fun `throttle is per-account`() {
        val clock = TestClock(60L * 60 * 1000)
        val a = FetchThrottle(
            ctx(), configFor("acct-a"),
            Constants.INBOX_V2_LAST_FETCH_TS_KEY,
            Constants.INBOX_V2_THROTTLE_WINDOW_MS,
            clock
        )
        val b = FetchThrottle(
            ctx(), configFor("acct-b"),
            Constants.INBOX_V2_LAST_FETCH_TS_KEY,
            Constants.INBOX_V2_THROTTLE_WINDOW_MS,
            clock
        )

        a.recordFetch()

        assertTrue(a.shouldThrottle())
        assertFalse(b.shouldThrottle())
    }

    @Test
    fun `two features with different pref keys do not collide`() {
        val cfg = configFor("acct-multi")
        val clock = TestClock(60L * 60 * 1000)
        val inbox = FetchThrottle(ctx(), cfg, "inbox_v2_last_fetch_ts", 5L * 60_000, clock)
        val recs = FetchThrottle(ctx(), cfg, "recs_v1_last_fetch_ts", 10L * 60_000, clock)

        inbox.recordFetch()

        assertTrue(inbox.shouldThrottle())
        assertFalse(recs.shouldThrottle())
    }

    @Test
    fun `throttle state persists across FetchThrottle instances`() {
        val cfg = configFor("acct-persistent")
        FetchThrottle(
            ctx(), cfg,
            Constants.INBOX_V2_LAST_FETCH_TS_KEY,
            Constants.INBOX_V2_THROTTLE_WINDOW_MS
        ).recordFetch()

        val second = FetchThrottle(
            ctx(), cfg,
            Constants.INBOX_V2_LAST_FETCH_TS_KEY,
            Constants.INBOX_V2_THROTTLE_WINDOW_MS
        )
        assertTrue(second.shouldThrottle())
    }
}
