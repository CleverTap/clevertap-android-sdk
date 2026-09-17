package com.clevertap.android.sdk.response

import android.content.Context
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.NdFCManager
import com.clevertap.android.sdk.displayunits.DisplayUnitCache
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.inapp.evaluation.NdEvaluationManager
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore
import com.clevertap.android.sdk.inapp.store.preference.NdStore
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Covers the single merged Display Units / ND processor: fcap meta ingestion
 * (absorbed from the former AdUnitResponse) + content delivery + user-switch handling.
 *
 * Runs under Robolectric (via [BaseTestCase]) because the content path uses `android.text.TextUtils`.
 */
class DisplayUnitResponseTest : BaseTestCase() {

    private lateinit var config: CleverTapInstanceConfig
    private lateinit var callbackManager: BaseCallbackManager
    private lateinit var controllerManager: ControllerManager
    private lateinit var storeRegistry: StoreRegistry
    private lateinit var ndTriggerManager: TriggerManager
    private lateinit var ndEvaluationManager: NdEvaluationManager
    private lateinit var ndStore: NdStore
    private lateinit var ndImpressionStore: ImpressionStore
    private lateinit var ndFCManager: NdFCManager
    private lateinit var cache: DisplayUnitCache
    private lateinit var context: Context
    private lateinit var response: DisplayUnitResponse

    override fun setUp() {
        super.setUp()
        config = mockk(relaxed = true)
        every { config.isAnalyticsOnly } returns false
        every { config.logger } returns mockk<Logger>(relaxed = true)
        every { config.accountId } returns "acc"

        callbackManager = mockk(relaxed = true)
        ndStore = mockk(relaxed = true)
        ndImpressionStore = mockk(relaxed = true)
        ndFCManager = mockk(relaxed = true)
        ndTriggerManager = mockk(relaxed = true)
        ndEvaluationManager = mockk(relaxed = true)
        cache = mockk(relaxed = true)
        context = mockk(relaxed = true)

        storeRegistry = mockk(relaxed = true)
        every { storeRegistry.ndStore } returns ndStore
        every { storeRegistry.ndImpressionStore } returns ndImpressionStore

        controllerManager = mockk(relaxed = true)
        every { controllerManager.ndFCManager } returns ndFCManager
        every { controllerManager.getOrCreateDisplayUnitCache() } returns cache

        response = DisplayUnitResponse(
            config, callbackManager, controllerManager, storeRegistry, ndTriggerManager, ndEvaluationManager
        )
    }

    // ---- meta (absorbed from AdUnitResponse) ----

    @Test
    fun `stores adUnit_notifs_ss metadata bundle`() {
        response.processResponse(JSONObject("""{"adUnit_notifs_ss":[{"ti":70001},{"ti":70002}]}"""), "", context)

        val slot = slot<List<JSONObject>>()
        verify { ndStore.storeServerSideNdMetaData(capture(slot)) }
        assertEquals(2, slot.captured.size)
    }

    @Test
    fun `applies ndmc and ndmp ceilings`() {
        response.processResponse(JSONObject("""{"ndmc":1,"ndmp":10}"""), "", context)
        verify { ndFCManager.updateLimits(10, 1) }
    }

    @Test
    fun `purges stale nd targets from all cap stores`() {
        response.processResponse(JSONObject("""{"adUnit_stale":["70001","70002"]}"""), "", context)
        verify { ndImpressionStore.clear("70001") }
        verify { ndImpressionStore.clear("70002") }
        verify { ndTriggerManager.removeTriggers("70001") }
        verify { ndFCManager.processResponse(any()) }
    }

    @Test
    fun `acks CG-suppressed app-launched stubs`() {
        val json = JSONObject(
            """{"adUnit_notifs_applaunched":[
                {"ti":70003,"wzrk_id":"70003_20260810","suppressed":true,"wzrk_cgId":0},
                {"ti":70004,"wzrk_id":"70004_20260810","type":"simple"}
            ]}"""
        )
        response.processResponse(json, "", context)

        val slot = slot<JSONObject>()
        verify(exactly = 1) { ndEvaluationManager.recordCgSuppressed(capture(slot)) }
        assertEquals("70003_20260810", slot.captured.optString("wzrk_id"))
    }

    @Test
    fun `no-op for analytics-only`() {
        every { config.isAnalyticsOnly } returns true
        response.processResponse(JSONObject("""{"ndmc":1,"ndmp":10}"""), "", context)
        verify(exactly = 0) { ndFCManager.updateLimits(any(), any()) }
    }

    // ---- content delivery + user switch ----

    @Test
    fun `delivers adUnit_notifs content to the host`() {
        val json = JSONObject("""{"adUnit_notifs":[{"wzrk_id":"u1","type":"simple"}]}""")
        response.processResponse(json, "", context)

        val slot = slot<ArrayList<CleverTapDisplayUnit>>()
        verify { cache.updateDisplayUnits(any()) }
        verify { callbackManager.notifyDisplayUnitsLoaded(capture(slot)) }
        assertEquals(1, slot.captured.size)
    }

    @Test
    fun `filters app-launched content by whenLimits and delivers only survivors`() {
        val json = JSONObject(
            """{"adUnit_notifs_applaunched":[
                {"ti":70001,"wzrk_id":"70001_20260810","type":"simple"},
                {"ti":70002,"wzrk_id":"70002_20260810","type":"simple"}
            ]}"""
        )
        // whenLimits filter keeps 70001, drops 70002 (over cap).
        every { ndEvaluationManager.retainAppLaunchedWithinLimits(any()) } answers {
            firstArg<List<JSONObject>>().filter { it.optString("ti") == "70001" }
        }

        response.processResponse(json, "", context)

        val slot = slot<ArrayList<CleverTapDisplayUnit>>()
        verify(exactly = 1) { ndEvaluationManager.retainAppLaunchedWithinLimits(any()) } // filter runs
        verify { callbackManager.notifyDisplayUnitsLoaded(capture(slot)) }
        assertEquals(1, slot.captured.size)                     // only the survivor is delivered
        assertEquals("70001_20260810", slot.captured[0].unitID)
    }

    @Test
    fun `does not deliver app-launched suppressed stubs to the whenLimits filter`() {
        val json = JSONObject(
            """{"adUnit_notifs_applaunched":[
                {"ti":70003,"wzrk_id":"70003_20260810","suppressed":true,"wzrk_cgId":0},
                {"ti":70004,"wzrk_id":"70004_20260810","type":"simple"}
            ]}"""
        )
        val slot = slot<List<JSONObject>>()
        every { ndEvaluationManager.retainAppLaunchedWithinLimits(capture(slot)) } answers { firstArg() }

        response.processResponse(json, "", context)

        // Only the non-suppressed unit reaches the filter; the CG stub is excluded (acked separately).
        verify(exactly = 1) { ndEvaluationManager.retainAppLaunchedWithinLimits(any()) }
        verify(exactly = 1) { ndEvaluationManager.recordCgSuppressed(any()) } // the CG stub is still acked
        assertEquals(1, slot.captured.size)
        assertEquals("70004", slot.captured[0].optString("ti"))
    }

    @Test
    fun `a throwing app-launched whenLimits filter degrades to delivery, not dropping the whole response`() {
        // A malformed advanced rule (e.g. onEvery limit=0 -> divide-by-zero) makes the filter throw.
        val json = JSONObject(
            """{
                "adUnit_notifs":[{"wzrk_id":"reg1","type":"simple"}],
                "adUnit_notifs_applaunched":[{"ti":70001,"wzrk_id":"70001_20260810","type":"simple"}]
            }""",
        )
        every { ndEvaluationManager.retainAppLaunchedWithinLimits(any()) } throws RuntimeException("divide by zero")

        response.processResponse(json, "", context)

        // Guarded: both the regular unit and the (un-filtered) app-launched unit are still delivered —
        // without the guard the throw would abort parseDisplayUnits and drop everything.
        val slot = slot<ArrayList<CleverTapDisplayUnit>>()
        verify { callbackManager.notifyDisplayUnitsLoaded(capture(slot)) }
        assertEquals(2, slot.captured.size)
    }

    @Test
    fun `clears the cache when the response carried content but the filter dropped everything`() {
        val json = JSONObject(
            """{"adUnit_notifs_applaunched":[{"ti":70001,"wzrk_id":"70001_20260810","type":"simple"}]}""",
        )
        every { ndEvaluationManager.retainAppLaunchedWithinLimits(any()) } returns emptyList()

        response.processResponse(json, "", context)

        // Content was present but nothing survived -> reset the cache (don't leave a suppressed unit
        // renderable via getAllDisplayUnits); no callback fires.
        verify(exactly = 1) { cache.updateDisplayUnits(match { it.isEmpty() }) }
        verify(exactly = 0) { callbackManager.notifyDisplayUnitsLoaded(any()) }
    }

    @Test
    fun `on user switch ingests meta but skips content delivery`() {
        val json = JSONObject("""{"ndmc":1,"ndmp":10,"adUnit_notifs":[{"wzrk_id":"u1","type":"simple"}]}""")

        response.processResponse(json, "", context, true)

        verify { ndFCManager.updateLimits(10, 1) }               // meta ingested
        verify(exactly = 0) { callbackManager.notifyDisplayUnitsLoaded(any()) } // content skipped
    }
}
