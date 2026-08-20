package com.clevertap.android.sdk.inapp.evaluation

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.inapp.store.preference.NdStore
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import com.clevertap.android.sdk.network.EndpointId
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
    private lateinit var manager: NdEvaluationManager

    @Before
    fun setUp() {
        triggersMatcher = mockk()
        ndTriggersManager = mockk(relaxed = true)
        ndLimitsMatcher = mockk()
        ndStore = mockk(relaxed = true)
        storeRegistry = mockk(relaxed = true)
        every { storeRegistry.ndStore } returns ndStore
        manager = NdEvaluationManager(triggersMatcher, ndTriggersManager, ndLimitsMatcher, storeRegistry)
    }

    @Test
    fun `evaluate votes eligible campaign into adUnit_eval`() {
        val inApp = JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, "70001")
        every { ndStore.readServerSideNdMetaData() } returns listOf(inApp)
        every { triggersMatcher.matchEvent(any(), any()) } returns true
        every { ndLimitsMatcher.matchWhenLimits(any(), any()) } returns true

        manager.evaluateOnEvent("Home Viewed", emptyMap(), null)

        assertTrue(manager.evaluatedNdCampaignIds.contains(70001L))
        verify { ndTriggersManager.increment("70001") }
        verify { ndStore.storeEvaluatedServerSideNdIds(any()) }
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
}
