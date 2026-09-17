package com.clevertap.android.sdk.response

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
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

    private val logger = mockk<Logger>(relaxed = true)

    private fun unit(json: JSONObject, wzrkId: String = "70001_20250101"): CleverTapDisplayUnit {
        val u = mockk<CleverTapDisplayUnit>(relaxed = true)
        every { u.jsonObject } returns json
        every { u.unitID } returns wzrkId
        return u
    }

    @Test
    fun `unmarked units pass through and the fcap manager is never touched`() {
        val ndfc = mockk<NdFCManager>()
        val units = arrayListOf(unit(JSONObject().put(Constants.NOTIFICATION_ID_TAG, "u1")))

        val out = NdFcapGate.filter(units, ndfc, logger, "acc")

        assertEquals(1, out.size)
        confirmVerified(ndfc) // no interaction at all with the fcap manager
    }

    @Test
    fun `fcap-marked unit is delivered when canShow allows, gated on the stable ti`() {
        val ndfc = mockk<NdFCManager>()
        every { ndfc.canShow(any(), any(), any(), any(), any(), any()) } returns true
        val json = JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, "70001").put(Constants.KEY_TLC, 5)

        val out = NdFcapGate.filter(arrayListOf(unit(json)), ndfc, logger, "acc")

        assertEquals(1, out.size)
        // Must gate on the campaign ti ("70001"), NOT the wzrk_id ("70001_20250101").
        verify(exactly = 1) { ndfc.canShow("70001", false, 5, -1, -1, false) }
        confirmVerified(ndfc)
    }

    @Test
    fun `fcap-marked unit is dropped when canShow denies`() {
        val ndfc = mockk<NdFCManager>()
        every { ndfc.canShow(any(), any(), any(), any(), any(), any()) } returns false
        val json = JSONObject().put(Constants.INAPP_ID_IN_PAYLOAD, "70002").put(Constants.KEY_TDC, 1)

        val out = NdFcapGate.filter(arrayListOf(unit(json, wzrkId = "70002_20250101")), ndfc, logger, "acc")

        assertEquals(0, out.size)
        verify(exactly = 1) { ndfc.canShow("70002", false, -1, 1, -1, false) }
        confirmVerified(ndfc)
    }

    @Test
    fun `null fcManager passes everything through`() {
        val units = arrayListOf(unit(JSONObject().put(Constants.KEY_TLC, 5)))

        assertEquals(1, NdFcapGate.filter(units, null, logger, "acc").size)
    }
}
