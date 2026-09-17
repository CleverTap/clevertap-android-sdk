package com.clevertap.android.sdk

import com.clevertap.android.sdk.displayunits.DisplayUnitCache
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.inapp.ImpressionManager
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.inapp.evaluation.LimitsMatcher
import com.clevertap.android.sdk.inapp.evaluation.NdEvaluationManager
import com.clevertap.android.sdk.inapp.evaluation.TriggersMatcher
import com.clevertap.android.sdk.inapp.store.preference.NdStore
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.utils.FakeClock
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Deterministic verification of the real ND fcap campaigns from scratch_17.json — limit exhaustion,
 * session breach, occurrence limits, and time-window reset — driven by a controllable [FakeClock].
 *
 * A "show" is driven through the **production public API**
 * [AnalyticsManager.pushDisplayUnitViewedEventForID] (the same call `CleverTapAPI` exposes): it looks
 * the unit up in the display-unit cache, gates on fcap-managed, and records the ND impression via a real
 * [NdFCManager]. The impression must be recorded under the stable `ti`
 * (not the `wzrk_id`) so the evaluator's whenLimits actually see it.
 *
 * The cache is an accumulating one (units persist across per-event deliveries), so the viewed lookup
 * always resolves; its behavior is asserted too. Lives in this package because AnalyticsManager's
 * constructor is package-private.
 */
class NdCampaignScenariosTest : BaseTestCase() {

    private lateinit var clock: FakeClock
    private lateinit var impressionManager: ImpressionManager
    private lateinit var manager: NdEvaluationManager
    private lateinit var analytics: AnalyticsManager
    private lateinit var cache: AccumulatingDisplayUnitCache
    private val metadata = mutableListOf<JSONObject>()

    // ti (== wzrk_id prefix) for all 11 campaigns, mirroring scratch_17.json.
    private val sadas = "1788769113"          // frequencyLimits: ever<=3, session<=1
    private val footer2 = "1788846338"        // frequencyLimits: 2 / hour
    private val footer4 = "1788846726"        // occurrenceLimits: onEvery 2 ; session<=3
    private val xhjjv = "1788771492"          // occurrenceLimits: onExactly 1 ; ever<=4, session<=2
    private val iin = "1789362099"            // frequencyLimits: 2 / minute
    private val footer1 = "1788845799"        // frequencyLimits: 3 / day
    private val vasegent2 = "1788772821"      // frequencyLimits: ever<=4
    private val footer3 = "1788846573"        // frequencyLimits: session<=3
    private val iitest = "1789366867"         // trigger number > 100 (op 0) ; occurrenceLimits: onEvery 1
    private val chargedEvery = "1789363015"   // event Charged ; occurrenceLimits: onEvery 1
    private val chargedExactly = "1788769682" // event Charged ; occurrenceLimits: onExactly 3

    private val allTis
        get() = listOf(sadas, footer2, footer4, xhjjv, iin, footer1, vasegent2, footer3, iitest, chargedEvery, chargedExactly)

    override fun setUp() {
        super.setUp()
        clock = FakeClock()
        val account = cleverTapInstanceConfig.accountId
        val deviceInfo = mockk<DeviceInfo>(relaxed = true).also { every { it.deviceID } returns "device" }

        val ndImpressionStore = StoreProvider.getInstance().provideNdImpressionStore(appCtx, "device", account)
        impressionManager = ImpressionManager(impressionStoreProvider = { ndImpressionStore }, clock = clock)
        val ndTriggersManager = TriggerManager(appCtx, account, deviceInfo, Constants.KEY_ND_TRIGGERS_PER_TARGET)
        val triggersMatcher = TriggersMatcher(mockk(relaxed = true))
        val limitsMatcher = LimitsMatcher(impressionManager, ndTriggersManager)

        val ndStore = mockk<NdStore>(relaxed = true)
        every { ndStore.readServerSideNdMetaData() } answers { metadata }
        val ndCountsStore = StoreProvider.getInstance().provideNdCountsStore(appCtx, "device", account)
        // One registry supplies both the evaluator's ndStore and the FC manager's counts store, exactly
        // as NdStoreProvider does in production.
        val storeRegistry = mockk<StoreRegistry>(relaxed = true).also {
            every { it.ndStore } returns ndStore
            every { it.ndCountsStore } returns ndCountsStore
        }
        manager = NdEvaluationManager(cleverTapInstanceConfig, triggersMatcher, ndTriggersManager, limitsMatcher, storeRegistry)

        // Real NdFCManager sharing the same ImpressionManager as the evaluator's LimitsMatcher, so a show
        // recorded via the manager is visible to the next evaluation's whenLimits.
        val ndFCManager = NdFCManager(cleverTapInstanceConfig, storeRegistry, impressionManager, MockCTExecutors(), clock)

        // Real AnalyticsManager: a show goes through pushDisplayUnitViewedEventForID (the public API),
        // which reads the unit from the cache and records the impression via the wired NdFCManager.
        cache = AccumulatingDisplayUnitCache()
        val coreState = MockCoreStateKotlin(cleverTapInstanceConfig)
        every { coreState.controllerManager.displayUnitCache } returns cache
        every { coreState.controllerManager.ndFCManager } returns ndFCManager
        analytics = AnalyticsManager(
            appCtx,
            cleverTapInstanceConfig,
            mockk<BaseEventQueueManager>(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            coreState.coreMetaData,
            coreState.deviceInfo,
            coreState.callbackManager,
            coreState.controllerManager,
            coreState.cTLockManager,
            clock,
            MockCTExecutors(),
            mockk(relaxed = true),
            mockk(relaxed = true),
        )

        // Advanced-rule metadata (what the evaluator reads).
        metadata += campaign(sadas, "sadas", freq = jarr(freq("ever", 3), freq("session", 1)))
        metadata += campaign(footer2, "Footer2", freq = jarr(freq("hours", 2, 1)))
        metadata += campaign(footer4, "Footer4", freq = jarr(freq("session", 3)), occ = jarr(occ("onEvery", 2)))
        metadata += campaign(xhjjv, "xhjjv", freq = jarr(freq("ever", 4), freq("session", 2)), occ = jarr(occ("onExactly", 1)))
        metadata += campaign(iin, "iin", freq = jarr(freq("minutes", 2, 1)))
        metadata += campaign(footer1, "Footer1", freq = jarr(freq("days", 3, 1)))
        metadata += campaign(vasegent2, "vasegent2", freq = jarr(freq("ever", 4)))
        metadata += campaign(footer3, "Footer3", freq = jarr(freq("session", 3)))
        metadata += campaignWithProperty(iitest, "iitest", "number", operator = 0, propertyValue = 100, occ = jarr(occ("onEvery", 1)))
        metadata += campaign(chargedEvery, "Charged", occ = jarr(occ("onEvery", 1)))
        metadata += campaign(chargedExactly, "Charged", occ = jarr(occ("onExactly", 3)))

        // Delivered content units in the cache (what the host renders/views), keyed by wzrk_id.
        allTis.forEach { ti -> cache.updateDisplayUnits(listOf(contentUnit(ti))) }
    }

    // ---- scenarios ----

    @Test
    fun `session cap breach - sadas suppressed after 1 per session, re-eligible next session`() {
        assertTrue(fire("sadas", sadas), "1st in session should vote")
        assertFalse(fire("sadas", sadas), "2nd in same session breaches session<=1")

        impressionManager.clearSessionData()
        assertTrue(fire("sadas", sadas), "new session resets the session cap")
    }

    @Test
    fun `ever cap exhaustion - sadas dies after 3 lifetime shows even across sessions`() {
        repeat(3) {
            assertTrue(fire("sadas", sadas), "show ${it + 1} of 3 should vote")
            impressionManager.clearSessionData() // isolate the ever<=3 cap from session<=1
        }
        assertFalse(fire("sadas", sadas), "4th ever breaches ever<=3")
    }

    @Test
    fun `hour window exhaustion and clock-driven reset - footer2 2 per hour`() {
        assertTrue(fire("Footer2", footer2))
        assertTrue(fire("Footer2", footer2))
        assertFalse(fire("Footer2", footer2), "3rd within the hour breaches 2/hour")

        clock.timeMillis += TimeUnit.HOURS.toMillis(1) + 1000
        assertTrue(fire("Footer2", footer2), "window reset after an hour")
    }

    @Test
    fun `minute window reset - iin 2 per minute`() {
        assertTrue(fire("iin", iin))
        assertTrue(fire("iin", iin))
        assertFalse(fire("iin", iin))

        clock.timeMillis += TimeUnit.MINUTES.toMillis(1) + 1000
        assertTrue(fire("iin", iin), "window reset after a minute")
    }

    @Test
    fun `onEvery 2 - footer4 votes on every 2nd trigger`() {
        assertFalse(fire("Footer4", footer4), "trigger 1")
        assertTrue(fire("Footer4", footer4), "trigger 2")
        assertFalse(fire("Footer4", footer4), "trigger 3")
        assertTrue(fire("Footer4", footer4), "trigger 4")
    }

    @Test
    fun `onExactly 1 - xhjjv votes only on the first trigger`() {
        assertTrue(fire("xhjjv", xhjjv), "trigger 1")
        assertFalse(fire("xhjjv", xhjjv), "trigger 2")
        assertFalse(fire("xhjjv", xhjjv), "trigger 3")
    }

    @Test
    fun `day cap exhaustion and next-day reset - footer1 3 per day`() {
        // perDay now honours the injected clock, so this is fully deterministic via FakeClock.
        assertTrue(fire("Footer1", footer1))
        assertTrue(fire("Footer1", footer1))
        assertTrue(fire("Footer1", footer1))
        assertFalse(fire("Footer1", footer1), "4th within the day breaches 3/day")

        // Cross two calendar days so today's shows fall outside perDay(id,1)'s "since yesterday 00:00" window.
        clock.advanceOneDay()
        clock.advanceOneDay()
        assertTrue(fire("Footer1", footer1), "re-eligible on a new day")
    }

    @Test
    fun `ever cap exhaustion - vasegent2 dies after 4 lifetime shows`() {
        repeat(4) { assertTrue(fire("vasegent2", vasegent2), "show ${it + 1} of 4 should vote") }
        assertFalse(fire("vasegent2", vasegent2), "5th ever breaches ever<=4")
    }

    @Test
    fun `session cap - footer3 3 per session, re-eligible next session`() {
        repeat(3) { assertTrue(fire("Footer3", footer3), "show ${it + 1} of 3 should vote") }
        assertFalse(fire("Footer3", footer3), "4th in same session breaches session<=3")

        impressionManager.clearSessionData()
        assertTrue(fire("Footer3", footer3), "new session resets the session cap")
    }

    @Test
    fun `property trigger - iitest votes only when number greater than 100`() {
        assertFalse(fire("iitest", iitest, mapOf("number" to 100)), "100 is not > 100")
        assertFalse(fire("iitest", iitest, mapOf("number" to 50)), "50 is not > 100")
        assertTrue(fire("iitest", iitest, mapOf("number" to 150)), "150 > 100 matches")
    }

    @Test
    fun `two Charged campaigns - onEvery1 fires each time, onExactly3 only on the 3rd`() {
        repeat(3) { round ->
            // Check the votes THIS fire appended (delta), not the accumulating list — no clear() needed.
            val votes = votesFrom { manager.evaluateOnEvent("Charged", emptyMap(), null) }

            assertTrue(votes.contains(chargedEvery.toLong()), "onEvery1 should vote on Charged #${round + 1}")
            assertEquals(
                round == 2,
                votes.contains(chargedExactly.toLong()),
                "onExactly3 should vote only on the 3rd Charged",
            )

            if (votes.contains(chargedEvery.toLong())) analytics.pushDisplayUnitViewedEventForID(wzrkId(chargedEvery))
            if (votes.contains(chargedExactly.toLong())) analytics.pushDisplayUnitViewedEventForID(wzrkId(chargedExactly))
        }
    }

    // ---- viewed event records the impression under the stable ti (regression guard) ----

    @Test
    fun `pushDisplayUnitViewedEventForID records the impression under ti so whenLimits see it`() {
        assertEquals(0, impressionManager.getImpressions(sadas).size)

        // Real public path: pushViewed(wzrk_id) -> cache lookup -> isFcapManaged -> didShow(ti) -> record.
        analytics.pushDisplayUnitViewedEventForID(wzrkId(sadas))

        // Recorded under the bare ti (NOT the wzrk_id), which is the key the evaluator's whenLimits read.
        assertEquals(1, impressionManager.getImpressions(sadas).size)
        assertEquals(0, impressionManager.getImpressions(wzrkId(sadas)).size)
    }

    // ---- display-unit cache behavior ----

    @Test
    fun `cache accumulates delivered units across deliveries`() {
        val before = cache.getAllDisplayUnits()!!.size
        cache.updateDisplayUnits(listOf(contentUnit("99999001"))) // a later, separate delivery

        val after = cache.getAllDisplayUnits()!!
        assertEquals(before + 1, after.size, "units must accumulate, not replace")
        assertTrue(after.any { it.unitID == wzrkId("99999001") })
    }

    @Test
    fun `cache getDisplayUnitForID resolves the delivered unit, null otherwise`() {
        assertEquals(wzrkId(sadas), cache.getDisplayUnitForID(wzrkId(sadas))?.unitID)
        assertNull(cache.getDisplayUnitForID("unknown_id"))
        assertNull(cache.getDisplayUnitForID(null))
        assertNull(cache.getDisplayUnitForID(""))
    }

    // ---- helpers ----

    /** Fires an event; if voted eligible, drives the real public show API which records the impression. */
    private fun fire(event: String, ti: String, props: Map<String, Any> = emptyMap()): Boolean {
        val voted = ti.toLong() in votesFrom { manager.evaluateOnEvent(event, props, null) }
        if (voted) analytics.pushDisplayUnitViewedEventForID(wzrkId(ti))
        return voted
    }

    /**
     * Runs [evaluate] and returns only the campaign ids it appended to the (accumulating) eval list —
     * i.e. the votes from *this* evaluation. Avoids clearing/mutating the manager's list between events.
     */
    private fun votesFrom(evaluate: () -> Unit): List<Long> {
        val before = manager.evaluatedNdCampaignIds.size
        evaluate()
        return manager.evaluatedNdCampaignIds.drop(before)
    }

    private fun wzrkId(ti: String) = "${ti}_20250101"

    /** A delivered content unit (host-visible), keyed by wzrk_id and carrying the stable ti + an fcap marker. */
    private fun contentUnit(ti: String): CleverTapDisplayUnit {
        val json = JSONObject()
            .put(Constants.NOTIFICATION_ID_TAG, wzrkId(ti)) // unitID = wzrk_id
            .put(Constants.INAPP_ID_IN_PAYLOAD, ti) // stable campaign id the fcaps key on
            .put(Constants.KEY_TYPE, "simple")
            .put(Constants.KEY_EXCLUDE_GLOBAL_CAPS, false) // presence marks it fcap-managed
        return CleverTapDisplayUnit.toDisplayUnit(json)
    }

    private fun campaign(ti: String, event: String, freq: JSONArray = JSONArray(), occ: JSONArray = JSONArray()) =
        JSONObject().apply {
            put(Constants.INAPP_ID_IN_PAYLOAD, ti)
            put(Constants.INAPP_WHEN_TRIGGERS, JSONArray().put(JSONObject().put("eventName", event)))
            put("frequencyLimits", freq)
            put("occurrenceLimits", occ)
        }

    private fun campaignWithProperty(
        ti: String,
        event: String,
        propertyName: String,
        operator: Int,
        propertyValue: Any,
        occ: JSONArray = JSONArray(),
    ) = JSONObject().apply {
        put(Constants.INAPP_ID_IN_PAYLOAD, ti)
        put(
            Constants.INAPP_WHEN_TRIGGERS,
            JSONArray().put(
                JSONObject()
                    .put("eventName", event)
                    .put(
                        "eventProperties",
                        JSONArray().put(
                            JSONObject()
                                .put("propertyName", propertyName)
                                .put("operator", operator)
                                .put("propertyValue", propertyValue),
                        ),
                    ),
            ),
        )
        put("frequencyLimits", JSONArray())
        put("occurrenceLimits", occ)
    }

    private fun freq(type: String, limit: Int, frequency: Int = 1) =
        JSONObject().put("type", type).put("limit", limit).put("frequency", frequency)

    private fun occ(type: String, limit: Int) = JSONObject().put("type", type).put("limit", limit)

    private fun jarr(vararg objs: JSONObject) = JSONArray().also { objs.forEach { o -> it.put(o) } }

    /** Test cache that ACCUMULATES delivered units (no reset) so per-event deliveries persist. */
    private class AccumulatingDisplayUnitCache : DisplayUnitCache {
        private val items = LinkedHashMap<String, CleverTapDisplayUnit>()

        override fun getAllDisplayUnits(): ArrayList<CleverTapDisplayUnit>? =
            if (items.isEmpty()) null else ArrayList(items.values)

        override fun getDisplayUnitForID(unitID: String?): CleverTapDisplayUnit? =
            if (unitID.isNullOrEmpty()) null else items[unitID]

        override fun updateDisplayUnits(displayUnits: List<CleverTapDisplayUnit>?) {
            displayUnits?.forEach { u -> u.unitID?.let { items[it] = u } } // accumulate, not replace
        }

        override fun reset() = items.clear()
    }
}
