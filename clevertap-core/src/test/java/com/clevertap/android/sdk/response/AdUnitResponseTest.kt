package com.clevertap.android.sdk.response

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.ControllerManager
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.NdFCManager
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.inapp.evaluation.NdEvaluationManager
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore
import com.clevertap.android.sdk.inapp.store.preference.NdStore
import com.clevertap.android.sdk.inapp.store.preference.StoreRegistry
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class AdUnitResponseTest {

    private lateinit var config: CleverTapInstanceConfig
    private lateinit var storeRegistry: StoreRegistry
    private lateinit var controllerManager: ControllerManager
    private lateinit var ndTriggerManager: TriggerManager
    private lateinit var ndEvaluationManager: NdEvaluationManager
    private lateinit var ndStore: NdStore
    private lateinit var ndImpressionStore: ImpressionStore
    private lateinit var ndFCManager: NdFCManager
    private lateinit var context: Context
    private lateinit var response: AdUnitResponse

    @Before
    fun setUp() {
        config = mockk(relaxed = true)
        every { config.isAnalyticsOnly } returns false
        every { config.logger } returns mockk<Logger>(relaxed = true)
        every { config.accountId } returns "acc"

        ndStore = mockk(relaxed = true)
        ndImpressionStore = mockk(relaxed = true)
        ndFCManager = mockk(relaxed = true)
        ndTriggerManager = mockk(relaxed = true)
        ndEvaluationManager = mockk(relaxed = true)
        context = mockk(relaxed = true)

        storeRegistry = mockk(relaxed = true)
        every { storeRegistry.ndStore } returns ndStore
        every { storeRegistry.ndImpressionStore } returns ndImpressionStore

        controllerManager = mockk(relaxed = true)
        every { controllerManager.ndFCManager } returns ndFCManager

        response = AdUnitResponse(config, storeRegistry, controllerManager, ndTriggerManager, ndEvaluationManager)
    }

    @Test
    fun `stores adUnit_notifs_ss metadata bundle`() {
        val json = JSONObject("""{"adUnit_notifs_ss":[{"ti":70001},{"ti":70002}]}""")

        response.processResponse(json, "", context)

        val slot = slot<List<JSONObject>>()
        verify { ndStore.storeServerSideNdMetaData(capture(slot)) }
        assertEquals(2, slot.captured.size)
    }

    @Test
    fun `applies ndmc and ndmp ceilings`() {
        val json = JSONObject("""{"ndmc":1,"ndmp":10}""")

        response.processResponse(json, "", context)

        verify { ndFCManager.updateLimits(context, 10, 1) }
    }

    @Test
    fun `purges stale nd targets from all cap stores`() {
        val json = JSONObject("""{"adUnit_stale":["70001","70002"]}""")

        response.processResponse(json, "", context)

        verify { ndImpressionStore.clear("70001") }
        verify { ndImpressionStore.clear("70002") }
        verify { ndTriggerManager.removeTriggers("70001") }
        verify { ndFCManager.processResponse(context, any()) }
    }

    @Test
    fun `acks CG-suppressed app-launched stubs and ignores real content`() {
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

        verify(exactly = 0) { ndFCManager.updateLimits(any(), any(), any()) }
    }
}
