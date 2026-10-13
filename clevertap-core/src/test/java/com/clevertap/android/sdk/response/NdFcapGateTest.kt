package com.clevertap.android.sdk.response

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.NdFCManager
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals

class NdFcapGateTest {

    private val logger = mockk<ILogger>(relaxed = true)

    // Real display unit built from JSON (no content array -> no Android deps), not a mock: the gate reads
    // jsonObject/unitID off it, so exercising the real parse is the point.
    private fun unit(json: JSONObject, wzrkId: String = "70001_20250101"): CleverTapDisplayUnit {
        json.put(Constants.NOTIFICATION_ID_TAG, wzrkId)
        if (!json.has(Constants.KEY_TYPE)) json.put(Constants.KEY_TYPE, "simple")
        return CleverTapDisplayUnit.toDisplayUnit(json)
    }

    @Test
    fun `non-regime units pass through and the fcap manager is never touched`() {
        val ndfc = mockk<NdFCManager>()
        // No isNdFcapEnabled -> not in the regime -> delivered uncapped, gate never consults the manager.
        val units = arrayListOf(unit(JSONObject().put(Constants.NOTIFICATION_ID_TAG, "u1")))

        val out = NdFcapGate.filter(units, ndfc, logger, "acc")

        assertEquals(1, out.size)
        confirmVerified(ndfc) // no interaction at all with the fcap manager
    }

    @Test
    fun `non-regime unit is not gated even if it carries a stray cap field`() {
        val ndfc = mockk<NdFCManager>()
        // mdc present but isNdFcapEnabled absent -> gate keys on the regime, not field presence -> pass through.
        val json = JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, "70009").put(Constants.INAPP_MAX_DISPLAY_COUNT, 1)

        val out = NdFcapGate.filter(arrayListOf(unit(json, wzrkId = "70009_20250101")), ndfc, logger, "acc")

        assertEquals(1, out.size)
        confirmVerified(ndfc) // non-regime -> manager never consulted
    }

    @Test
    fun `regime unit is delivered when canShow allows, gated on the stable ti`() {
        val ndfc = mockk<NdFCManager>()
        every { ndfc.canShow(any(), any(), any(), any()) } returns true
        // In the regime (isNdFcapEnabled) -> reaches the session gate. No efc/excludeGlobalFCaps -> not exempt;
        // the session-only canShow no longer reads tlc/tdc (server-owned on V2), tlc here is extra decoration.
        val json = JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, "70001")
            .put(Constants.KEY_IS_ND_FCAP_ENABLED, true).put(Constants.KEY_TLC, 5)

        val out = NdFcapGate.filter(arrayListOf(unit(json)), ndfc, logger, "acc")

        assertEquals(1, out.size)
        // Must gate on the campaign ti ("70001"), NOT the wzrk_id ("70001_20250101"); mdc=-1 (no mdc key).
        verify(exactly = 1) { ndfc.canShow("70001", false, -1, false) }
        confirmVerified(ndfc)
    }

    @Test
    fun `regime unit with no legacy cap fields is still gated`() {
        val ndfc = mockk<NdFCManager>()
        every { ndfc.canShow(any(), any(), any(), any()) } returns true
        // Only isNdFcapEnabled, no efc/tlc/tdc/mdc/excludeGlobalFCaps -> the old field-presence gate would have
        // skipped it; the regime gate still session-checks it.
        val json = JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, "70001").put(Constants.KEY_IS_ND_FCAP_ENABLED, true)

        val out = NdFcapGate.filter(arrayListOf(unit(json)), ndfc, logger, "acc")

        assertEquals(1, out.size)
        verify(exactly = 1) { ndfc.canShow("70001", false, -1, false) }
        confirmVerified(ndfc)
    }

    @Test
    fun `regime unit is dropped when canShow denies`() {
        val ndfc = mockk<NdFCManager>()
        every { ndfc.canShow(any(), any(), any(), any()) } returns false
        val json = JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, "70002")
            .put(Constants.KEY_IS_ND_FCAP_ENABLED, true).put(Constants.KEY_TDC, 1)

        val out = NdFcapGate.filter(arrayListOf(unit(json, wzrkId = "70002_20250101")), ndfc, logger, "acc")

        assertEquals(0, out.size)
        verify(exactly = 1) { ndfc.canShow("70002", false, -1, false) }
        confirmVerified(ndfc)
    }

    @Test
    fun `null fcManager passes everything through`() {
        val units = arrayListOf(unit(JSONObject().put(Constants.KEY_TLC, 5)))

        assertEquals(1, NdFcapGate.filter(units, null, logger, "acc").size)
    }
}
