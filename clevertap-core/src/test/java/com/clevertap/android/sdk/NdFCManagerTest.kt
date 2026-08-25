package com.clevertap.android.sdk

import com.clevertap.android.sdk.inapp.ImpressionManager
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.utils.FakeClock
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NdFCManagerTest : BaseTestCase() {

    private lateinit var impressionManager: ImpressionManager
    private lateinit var clock: FakeClock

    override fun setUp() {
        super.setUp()
        impressionManager = mockk(relaxed = true) // perSession / perSessionTotal -> 0
        clock = FakeClock()
    }

    @Test
    fun `canShow returns true for empty id`() {
        val fc = create()
        assertTrue(fc.canShow("", false, -1, -1, -1, false))
    }

    @Test
    fun `canShow returns false when frequency limits are maxed`() {
        val fc = create()
        assertFalse(fc.canShow("70001", false, -1, -1, -1, true))
    }

    @Test
    fun `canShow allows an uncapped target`() {
        val fc = create()
        fc.updateLimits(10, 10) // don't let global defaults (1) interfere
        assertTrue(fc.canShow("70001", false, -1, -1, -1, false))
    }

    @Test
    fun `excludeFromCaps bypasses counter caps`() {
        val fc = create()
        fc.updateLimits(10, 10)
        fc.didShow("70001") // today/lifetime = 1
        // per-target daily cap of 1 would deny, but excludeFromCaps short-circuits
        assertFalse(fc.canShow("70001", false, -1, 1, -1, false))
        assertTrue(fc.canShow("70001", true, -1, 1, -1, false))
    }

    @Test
    fun `per-target daily cap denies once reached`() {
        val fc = create()
        fc.updateLimits(10, 10)
        fc.didShow("70001") // today = 1
        assertFalse(fc.canShow("70001", false, -1, 1, -1, false)) // 1 >= 1
        assertTrue(fc.canShow("70001", false, -1, 2, -1, false))  // 1 < 2
    }

    @Test
    fun `didShow increments ndtlc counters and shown-today`() {
        val fc = create()
        fc.updateLimits(10, 10)
        fc.didShow("70001")
        fc.didShow("70001")

        assertEquals(2, fc.shownTodayCount)
        val counts = fc.getNdCounts()!!
        assertEquals(1, counts.length())
        val entry = counts.getJSONArray(0)
        assertEquals("70001", entry.getString(0))
        assertEquals(2, entry.getInt(1)) // today
        assertEquals(2, entry.getInt(2)) // lifetime
    }

    private fun create(deviceId: String = "deviceId"): NdFCManager {
        val countsStore = StoreProvider.getInstance()
            .provideNdCountsStore(appCtx, deviceId, cleverTapInstanceConfig.accountId)
        return NdFCManager(
            config = cleverTapInstanceConfig,
            countsStore = countsStore,
            impressionManager = impressionManager,
            executors = MockCTExecutors(),
            clock = clock,
        )
    }
}
