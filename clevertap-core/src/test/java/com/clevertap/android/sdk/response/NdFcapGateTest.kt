package com.clevertap.android.sdk.response

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.NdFCManager
import com.clevertap.android.sdk.displayunits.model.CleverTapDisplayUnit
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.json.JSONObject
import org.junit.Test
import kotlin.test.assertEquals

class NdFcapGateTest {

    private val logger = mockk<Logger>(relaxed = true)

    private fun unit(json: JSONObject, id: String = "u1"): CleverTapDisplayUnit {
        val u = mockk<CleverTapDisplayUnit>(relaxed = true)
        every { u.jsonObject } returns json
        every { u.unitID } returns id
        return u
    }

    @Test
    fun `unmarked units pass through and are never cap-checked`() {
        val ndfc = mockk<NdFCManager>()
        val units = arrayListOf(unit(JSONObject().put(Constants.NOTIFICATION_ID_TAG, "u1")))

        val out = NdFcapGate.filter(units, ndfc, logger, "acc")

        assertEquals(1, out.size)
        verify(exactly = 0) { ndfc.canShow(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `fcap-marked unit is delivered when canShow allows`() {
        val ndfc = mockk<NdFCManager>()
        every { ndfc.canShow(any(), any(), any(), any(), any(), any()) } returns true
        val units = arrayListOf(unit(JSONObject().put(Constants.KEY_TLC, 5)))

        assertEquals(1, NdFcapGate.filter(units, ndfc, logger, "acc").size)
    }

    @Test
    fun `fcap-marked unit is dropped when canShow denies`() {
        val ndfc = mockk<NdFCManager>()
        every { ndfc.canShow(any(), any(), any(), any(), any(), any()) } returns false
        val units = arrayListOf(unit(JSONObject().put(Constants.KEY_TDC, 1)))

        assertEquals(0, NdFcapGate.filter(units, ndfc, logger, "acc").size)
    }

    @Test
    fun `null fcManager passes everything through`() {
        val units = arrayListOf(unit(JSONObject().put(Constants.KEY_TLC, 5)))

        assertEquals(1, NdFcapGate.filter(units, null, logger, "acc").size)
    }
}
