package com.clevertap.android.sdk.pushnotification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM unit tests for the Live Update Mode A/B routing + in-place id derivation. */
class LiveActivityRouterTest {

    @Test
    fun `pt_id present selects SDK_RENDER even when a factory is registered`() {
        assertEquals(LiveActivityMode.SDK_RENDER, LiveActivityRouter.mode(isPtMode = true, hasFactory = true))
        assertEquals(LiveActivityMode.SDK_RENDER, LiveActivityRouter.mode(isPtMode = true, hasFactory = false))
    }

    @Test
    fun `no pt_id with a factory selects FACTORY`() {
        assertEquals(LiveActivityMode.FACTORY, LiveActivityRouter.mode(isPtMode = false, hasFactory = true))
    }

    @Test
    fun `no pt_id and no factory falls through to SDK_RENDER`() {
        assertEquals(LiveActivityMode.SDK_RENDER, LiveActivityRouter.mode(isPtMode = false, hasFactory = false))
    }

    @Test
    fun `stableId is deterministic and non-negative`() {
        val first = LiveActivityRouter.stableId("order-123")
        assertNotNull(first)
        assertEquals(first, LiveActivityRouter.stableId("order-123"))
        assertTrue(first!! >= 0)
    }

    @Test
    fun `stableId is null for null or empty key`() {
        assertNull(LiveActivityRouter.stableId(null))
        assertNull(LiveActivityRouter.stableId(""))
    }

    @Test
    fun `different keys derive different ids`() {
        assertNotEquals(LiveActivityRouter.stableId("a"), LiveActivityRouter.stableId("b"))
    }
}
