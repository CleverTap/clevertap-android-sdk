package com.clevertap.android.sdk

import com.clevertap.android.sdk.inapp.ImpressionManager
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.utils.FakeClock
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONObject
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
        assertTrue(fc.canShow("", false, -1, false))
    }

    @Test
    fun `canShow returns false when frequency limits are maxed`() {
        val fc = create()
        assertFalse(fc.canShow("70001", false, -1, true))
    }

    @Test
    fun `canShow allows a target within session caps`() {
        val fc = create()
        fc.updateLimits(10, 10)
        assertTrue(fc.canShow("70001", false, -1, false))
    }

    @Test
    fun `excludeFromCaps bypasses the session cap`() {
        val fc = create()
        fc.updateLimits(10, 10)
        every { impressionManager.perSession("70001") } returns 5 // per-target session already spent
        // per-target session cap of 1 would deny, but excludeFromCaps short-circuits
        assertFalse(fc.canShow("70001", false, 1, false))
        assertTrue(fc.canShow("70001", true, 1, false))
    }

    @Test
    fun `per-target session cap denies once reached`() {
        val fc = create()
        fc.updateLimits(10, 10)
        every { impressionManager.perSession("70001") } returns 1
        assertFalse(fc.canShow("70001", false, 1, false)) // 1 >= 1
        assertTrue(fc.canShow("70001", false, 2, false))  // 1 < 2
    }

    @Test
    fun `didShow increments ndtlc counters and shown-today`() {
        val fc = create()
        fc.updateLimits(10, 10)
        fc.didShow("70001", true)
        fc.didShow("70001", true)

        assertEquals(2, fc.shownTodayCount)
        val counts = fc.getNdCounts()!!
        assertEquals(1, counts.length())
        val entry = counts.getJSONArray(0)
        assertEquals("70001", entry.getString(0))
        assertEquals(2, entry.getInt(1)) // today
        assertEquals(2, entry.getInt(2)) // lifetime
    }

    @Test
    fun `didShow with countsTowardCaps false records session impression but not ndmp or ndtlc`() {
        val fc = create()
        fc.updateLimits(10, 10)
        fc.didShow("70001", false) // exempt: flag off/absent or excludeGlobalFCaps

        // Session impression still recorded (session caps + advanced frequencyLimits are always SDK-owned).
        verify { impressionManager.recordImpression("70001") }
        // ...but the server-reported global/per-target counters are NOT bumped for an exempt unit.
        assertEquals(0, fc.shownTodayCount)
        assertEquals(0, fc.getNdCounts()!!.length())
    }

    @Test
    fun `globalCapRemaining is the lower of daily and session headroom`() {
        val fc = create()
        fc.updateLimits(10, 3) // ndmp=10 daily, ndmc=3 session; impressionManager.perSessionTotal()->0
        assertEquals(3, fc.globalCapRemaining()) // min(10-0, 3-0) = 3, session binds

        fc.didShow("70001", true)
        fc.didShow("70001", true) // shownToday=2
        assertEquals(3, fc.globalCapRemaining()) // min(10-2=8, 3-0=3) = 3, session still binds
    }

    @Test
    fun `globalCapRemaining lets the daily cap bind and floors at zero`() {
        val fc = create()
        fc.updateLimits(2, 100) // daily 2 binds
        fc.didShow("a", true)
        fc.didShow("b", true) // shownToday=2
        assertEquals(0, fc.globalCapRemaining()) // min(2-2=0, 100-0) = 0
    }

    @Test
    fun `regime helpers follow the isNdFcapEnabled model`() {
        // non-regime: flag absent/false -> not in regime, nothing counts
        assertFalse(NdFCManager.inRegime(JSONObject()))
        assertFalse(NdFCManager.countsTowardCaps(JSONObject()))

        // regime accepts boolean OR positive int
        assertTrue(NdFCManager.inRegime(JSONObject().put(Constants.KEY_IS_ND_FCAP_ENABLED, true)))
        assertTrue(NdFCManager.inRegime(JSONObject().put(Constants.KEY_IS_ND_FCAP_ENABLED, 1)))

        // regime + not globally excluded -> counts; efc also counts (DELIVER_COUNTED)
        assertTrue(NdFCManager.countsTowardCaps(JSONObject().put(Constants.KEY_IS_ND_FCAP_ENABLED, true)))
        assertTrue(
            NdFCManager.countsTowardCaps(
                JSONObject().put(Constants.KEY_IS_ND_FCAP_ENABLED, true).put(Constants.KEY_EFC, 1),
            ),
        )

        // excludeGlobalFCaps -> excluded from counting, but still exclude-from-caps
        val exempt = JSONObject().put(Constants.KEY_IS_ND_FCAP_ENABLED, true).put(Constants.KEY_EXCLUDE_GLOBAL_CAPS, 1)
        assertFalse(NdFCManager.countsTowardCaps(exempt))
        assertTrue(NdFCManager.isExcludeFromCaps(exempt))
        assertTrue(NdFCManager.isExcludeFromCaps(JSONObject().put(Constants.KEY_EFC, 1)))
    }

    private fun create(deviceId: String = "deviceId"): NdFCManager {
        // Real counts store (its own prefs file); only the registry locator is mocked to hand it back —
        // the manager exercises the real counting logic, matching how NdStoreProvider supplies it in prod.
        val countsStore = StoreProvider.getInstance()
            .provideNdCountsStore(appCtx, deviceId, cleverTapInstanceConfig.accountId)
        val storeRegistry = mockk<StoreRegistry>(relaxed = true)
        every { storeRegistry.ndCountsStore } returns countsStore
        return NdFCManager(
            config = cleverTapInstanceConfig,
            storeRegistry = storeRegistry,
            impressionManager = impressionManager,
            executors = MockCTExecutors(),
            clock = clock,
        )
    }
}
