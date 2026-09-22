package com.clevertap.android.sdk.inapp.evaluation

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.inapp.store.preference.NdStore
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.network.EndpointId
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NdEvaluationManagerTest {

    private lateinit var triggersMatcher: TriggersMatcher
    private lateinit var ndTriggersManager: TriggerManager
    private lateinit var ndLimitsMatcher: LimitsMatcher
    private lateinit var storeRegistry: StoreRegistry
    private lateinit var ndStore: NdStore
    private lateinit var config: CleverTapInstanceConfig
    private lateinit var manager: NdEvaluationManager

    @Before
    fun setUp() {
        config = mockk(relaxed = true) // relaxed -> logger calls are no-ops
        triggersMatcher = mockk()
        ndTriggersManager = mockk(relaxed = true)
        ndLimitsMatcher = mockk()
        ndStore = mockk(relaxed = true)
        storeRegistry = mockk(relaxed = true)
        every { storeRegistry.ndStore } returns ndStore
        manager = NdEvaluationManager(config, triggersMatcher, ndTriggersManager, ndLimitsMatcher, storeRegistry)
    }

    @Test
    fun `evaluate votes eligible campaign into adUnit_eval`() {
        val inApp = JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, "70001")
        every { ndStore.readServerSideNdMetaData() } returns listOf(inApp)
        every { triggersMatcher.matchEvent(any(), any()) } returns true
        every { ndLimitsMatcher.matchWhenLimits(any(), any()) } returns true

        manager.evaluateOnEvent("Home Viewed", emptyMap(), null)

        assertTrue(manager.evaluatedNdCampaignIds.contains(70001L))
        verify(exactly = 1) { ndStore.readServerSideNdMetaData() }
        verify(exactly = 1) { ndTriggersManager.increment("70001") }
        verify(exactly = 1) { ndStore.storeEvaluatedServerSideNdIds(any()) }
        confirmVerified(ndTriggersManager, ndStore) // exactly these interactions, nothing else
    }

    @Test
    fun `evaluate does not vote when whenLimits fail`() {
        val inApp = JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, "70001")
        every { ndStore.readServerSideNdMetaData() } returns listOf(inApp)
        every { triggersMatcher.matchEvent(any(), any()) } returns true
        every { ndLimitsMatcher.matchWhenLimits(any(), any()) } returns false

        manager.evaluateOnEvent("e", emptyMap(), null)

        assertTrue(manager.evaluatedNdCampaignIds.isEmpty())
        verify { ndTriggersManager.increment("70001") } // trigger still counted
    }

    @Test
    fun `evaluate appends a concurrent re-vote without dedup`() {
        val inApp = JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, "70001")
        every { ndStore.readServerSideNdMetaData() } returns listOf(inApp)
        every { triggersMatcher.matchEvent(any(), any()) } returns true
        every { ndLimitsMatcher.matchWhenLimits(any(), any()) } returns true

        // Same campaign eligible on two events (e.g. a re-vote while a prior send is in flight).
        manager.evaluateOnEvent("e1", emptyMap(), null)
        manager.evaluateOnEvent("e2", emptyMap(), null)

        // Must NOT be de-duped — onSentHeaders removes only what was sent.
        assertEquals(listOf(70001L, 70001L), manager.evaluatedNdCampaignIds)
    }

    @Test
    fun `loadEvaluatedAndSuppressedNdIds restores persisted int-range ids`() {
        // ti's are epoch-second ids that org.json parses as Integer; the load must not drop them.
        every { ndStore.readEvaluatedServerSideNdIds() } returns JSONArray("[70001,70002]")
        every { ndStore.readSuppressedNdIds() } returns JSONArray()

        manager.loadEvaluatedAndSuppressedNdIds()

        assertEquals(listOf(70001L, 70002L), manager.evaluatedNdCampaignIds)
    }

    @Test
    fun `evaluate does not vote when trigger does not match`() {
        val inApp = JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, "70001")
        every { ndStore.readServerSideNdMetaData() } returns listOf(inApp)
        every { triggersMatcher.matchEvent(any(), any()) } returns false

        manager.evaluateOnEvent("e", emptyMap(), null)

        assertTrue(manager.evaluatedNdCampaignIds.isEmpty())
    }

    @Test
    fun `onAttachHeaders attaches adUnit_eval only for A1`() {
        manager.evaluatedNdCampaignIds.add(70001L)

        val header = manager.onAttachHeaders(EndpointId.ENDPOINT_A1)
        assertEquals(
            JSONArray().put(70001L).toString(),
            header!!.optJSONArray(Constants.ND_SS_EVAL_META).toString()
        )
        assertNull(manager.onAttachHeaders(EndpointId.ENDPOINT_SPIKY))
    }

    @Test
    fun `onSentHeaders removes exactly the sent eval ids`() {
        manager.evaluatedNdCampaignIds.addAll(listOf(70001L, 70002L))
        val sent = JSONObject().put(Constants.ND_SS_EVAL_META, JSONArray().put(70001L))

        manager.onSentHeaders(sent, EndpointId.ENDPOINT_A1)

        assertEquals(listOf(70002L), manager.evaluatedNdCampaignIds)
    }

    @Test
    fun `recordCgSuppressed adds a bare ack entry`() {
        val stub = JSONObject()
            .put(Constants.NOTIFICATION_ID_TAG, "70004_20260810")
            .put(Constants.INAPP_WZRK_CGID, 0)

        manager.recordCgSuppressed(stub)

        assertEquals(1, manager.suppressedNdCampaigns.size)
        assertEquals("70004_20260810", manager.suppressedNdCampaigns[0][Constants.NOTIFICATION_ID_TAG])
        assertEquals("wzrk_default", manager.suppressedNdCampaigns[0][Constants.INAPP_WZRK_PIVOT])
    }

    // ---- App-Launched content-in-advance whenLimits filter ----
    // Rules are read INLINE from each app-launched payload (contract §5.2 excludes app-launched campaigns
    // from adUnit_notifs_ss), mirroring in-app. So these fixtures carry frequencyLimits/occurrenceLimits
    // on the unit itself, NOT in the ss-bundle.

    /** An app-launched unit carrying an inline advanced rule (so EvalRules.whenLimits is non-empty). */
    private fun appLaunchedUnitWithRule(ti: String) = JSONObject()
        .put(Constants.INAPP_ID_IN_PAYLOAD, ti)
        .put(
            Constants.INAPP_FC_LIMITS,
            JSONArray().put(JSONObject().put("type", "session").put("limit", 1)),
        )

    @Test
    fun `retainAppLaunchedWithinLimits keeps units within whenLimits and drops over-cap ones`() {
        every { ndLimitsMatcher.matchWhenLimits(any(), "70001") } returns true
        every { ndLimitsMatcher.matchWhenLimits(any(), "70002") } returns false

        val within = appLaunchedUnitWithRule("70001")
        val overCap = appLaunchedUnitWithRule("70002")

        val kept = manager.retainAppLaunchedWithinLimits(listOf(within, overCap))

        assertEquals(listOf(within), kept)
        // Trigger is counted for BOTH — occurrence limits advance even when the unit is ultimately dropped.
        verify(exactly = 1) { ndTriggersManager.increment("70001") }
        verify(exactly = 1) { ndTriggersManager.increment("70002") }
        verify(exactly = 1) { ndLimitsMatcher.matchWhenLimits(any(), "70001") }
        verify(exactly = 1) { ndLimitsMatcher.matchWhenLimits(any(), "70002") }
        confirmVerified(ndTriggersManager, ndLimitsMatcher) // exactly these interactions, nothing else
    }

    @Test
    fun `retainAppLaunchedWithinLimits passes units with no inline rules through untouched`() {
        // A simple campaign: full content payload, but no frequencyLimits/occurrenceLimits inline.
        val simple = JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, "88888")

        val kept = manager.retainAppLaunchedWithinLimits(listOf(simple))

        assertEquals(listOf(simple), kept)
        confirmVerified(ndTriggersManager, ndLimitsMatcher) // no inline rules -> evaluator untouched
    }

    @Test
    fun `retainAppLaunchedWithinLimits does not consult the ss-bundle`() {
        // App-launched campaigns are excluded from adUnit_notifs_ss (contract §5.2); the filter must
        // rely solely on inline rules and never read the bundle.
        val unit = appLaunchedUnitWithRule("70001")
        every { ndLimitsMatcher.matchWhenLimits(any(), "70001") } returns true

        manager.retainAppLaunchedWithinLimits(listOf(unit))

        verify(exactly = 0) { ndStore.readServerSideNdMetaData() }
    }
}
