package com.clevertap.android.sdk.displayunits

import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.inapp.evaluation.NdEvaluationManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies ND evaluation is driven by its own controller (SDK-6055 Phase 10): each hook merges the
 * app-launched fields with the event props, delegates to [NdEvaluationManager], and swallows faults.
 */
class NativeDisplayControllerTest {

    private lateinit var config: CleverTapInstanceConfig
    private lateinit var deviceInfo: DeviceInfo
    private lateinit var ndEvaluationManager: NdEvaluationManager
    private lateinit var controller: NativeDisplayController

    @Before
    fun setUp() {
        config = mockk(relaxed = true)
        every { config.logger } returns mockk<Logger>(relaxed = true)
        every { config.accountId } returns "acc"
        deviceInfo = mockk(relaxed = true)
        every { deviceInfo.appLaunchedFields } returns JSONObject().put("os", "Android")
        ndEvaluationManager = mockk(relaxed = true)
        controller = NativeDisplayController(config, deviceInfo, ndEvaluationManager)
    }

    @Test
    fun `onQueueEvent delegates with merged app fields`() {
        val props = slot<Map<String, Any>>()
        controller.onQueueEvent("Home", mapOf("k" to 1), null)

        verify { ndEvaluationManager.evaluateOnEvent("Home", capture(props), null) }
        assertEquals(1, props.captured["k"])
        assertEquals("Android", props.captured["os"]) // app-launched field merged in
    }

    @Test
    fun `onQueueChargedEvent delegates`() {
        val items = listOf(mapOf<String, Any>("id" to "sku1"))
        controller.onQueueChargedEvent(mapOf("amount" to 10), items, null)
        verify { ndEvaluationManager.evaluateOnChargedEvent(any(), items, null) }
    }

    @Test
    fun `onQueueProfileEvent delegates`() {
        val changes = mapOf("age" to mapOf<String, Any?>("newValue" to 30))
        controller.onQueueProfileEvent(changes, null)
        verify { ndEvaluationManager.evaluateOnUserAttributeChange(changes, null, any()) }
    }

    @Test
    fun `an ND evaluation fault is swallowed`() {
        every { ndEvaluationManager.evaluateOnEvent(any(), any(), any()) } throws RuntimeException("boom")
        // Must not propagate — ND must never break the event pipeline.
        controller.onQueueEvent("Home", emptyMap(), null)
        assertTrue(true)
    }
}
