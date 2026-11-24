package com.clevertap.android.sdk.pushnotification

import android.content.Context
import android.os.Bundle
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.utils.Clock
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LiveUpdateOrderGuardTest : BaseTestCase() {

    private var now = 1_000_000L
    private lateinit var guard: LiveUpdateOrderGuard

    @Before
    override fun setUp() {
        super.setUp()
        val prefs = appCtx.getSharedPreferences("order_guard_test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val clock = mockk<Clock> { every { currentTimeSeconds() } answers { now } }
        guard = LiveUpdateOrderGuard(prefs = prefs, clock = clock)
    }

    private fun push(
        event: String = "update",
        seq: Long? = null,
        timestamp: Long? = null,
        activityId: String? = "match_arg_fra"
    ) = Bundle().apply {
        activityId?.let { putString(Constants.WZRK_LIVE_UPDATE_ID, it) }
        putString(Constants.WZRK_LIVE_UPDATE_EVENT, event)
        seq?.let { putString(Constants.WZRK_LIVE_UPDATE_SEQ, "$it") }
        timestamp?.let { putString(Constants.LIVE_UPDATE_TIMESTAMP, "$it") }
    }

    @Test
    fun `older update after a newer one is dropped`() {
        assertTrue(guard.accept(push(seq = 3)))
        assertFalse(guard.accept(push(seq = 2)))
        assertTrue(guard.accept(push(seq = 4)))
    }

    @Test
    fun `equal order is accepted`() {
        assertTrue(guard.accept(push(seq = 3)))
        assertTrue(guard.accept(push(seq = 3)))
    }

    @Test
    fun `timestamp is the fallback when seq is absent`() {
        assertTrue(guard.accept(push(timestamp = 200)))
        assertFalse(guard.accept(push(timestamp = 100)))
    }

    @Test
    fun `end is never dropped and blocks every later update`() {
        assertTrue(guard.accept(push(seq = 5)))
        assertTrue(guard.accept(push(event = "end", seq = 4)))
        assertFalse(guard.accept(push(seq = 9)))
    }

    @Test
    fun `no order value or no activity id means no guard`() {
        assertTrue(guard.accept(push(seq = 5)))
        assertTrue(guard.accept(push()))
        assertTrue(guard.accept(push(seq = 1, activityId = null)))
    }

    @Test
    fun `activities are tracked independently`() {
        assertTrue(guard.accept(push(seq = 5)))
        assertTrue(guard.accept(push(seq = 1, activityId = "match_bra_ger")))
    }

    @Test
    fun `entries older than retention are pruned`() {
        assertTrue(guard.accept(push(seq = 5)))
        now += LiveUpdateOrderGuard.RETENTION_SECONDS + 1
        // Writing another activity prunes the stale one, so an old seq is accepted again.
        assertTrue(guard.accept(push(seq = 1, activityId = "other")))
        assertTrue(guard.accept(push(seq = 1)))
    }
}
