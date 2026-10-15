package com.clevertap.android.sdk.pushnotification

import android.os.Bundle
import com.clevertap.android.sdk.Constants
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure-JVM unit tests for the Live Update Mode A/B routing + in-place id derivation. */
class LiveUpdateRouterTest {

    private fun extras(wzrkLa: String?) = mockk<Bundle> {
        every { getString(Constants.WZRK_LIVE_UPDATE) } returns wzrkLa
    }

    @Test
    fun `isLiveUpdate is true for wzrk_la true in any case`() {
        assertTrue(LiveUpdateRouter.isLiveUpdate(extras("true")))
        assertTrue(LiveUpdateRouter.isLiveUpdate(extras("TRUE")))
    }

    @Test
    fun `isLiveUpdate is false for a missing, other or null bundle`() {
        assertFalse(LiveUpdateRouter.isLiveUpdate(extras(null)))
        assertFalse(LiveUpdateRouter.isLiveUpdate(extras("false")))
        assertFalse(LiveUpdateRouter.isLiveUpdate(null))
    }

    @Test
    fun `pt_id present selects SDK_RENDER even when a factory is registered`() {
        assertEquals(LiveUpdateMode.SDK_RENDER, LiveUpdateRouter.mode(isPtMode = true, hasFactory = true))
        assertEquals(LiveUpdateMode.SDK_RENDER, LiveUpdateRouter.mode(isPtMode = true, hasFactory = false))
    }

    @Test
    fun `no pt_id with a factory selects FACTORY`() {
        assertEquals(LiveUpdateMode.FACTORY, LiveUpdateRouter.mode(isPtMode = false, hasFactory = true))
    }

    @Test
    fun `no pt_id and no factory falls through to SDK_RENDER`() {
        assertEquals(LiveUpdateMode.SDK_RENDER, LiveUpdateRouter.mode(isPtMode = false, hasFactory = false))
    }

    @Test
    fun `stableId is deterministic and non-negative`() {
        val first = LiveUpdateRouter.stableId("order-123")
        assertNotNull(first)
        assertEquals(first, LiveUpdateRouter.stableId("order-123"))
        assertTrue(first!! >= 0)
    }

    @Test
    fun `stableId is null for null or empty key`() {
        assertNull(LiveUpdateRouter.stableId(null))
        assertNull(LiveUpdateRouter.stableId(""))
    }

    @Test
    fun `different keys derive different ids`() {
        assertNotEquals(LiveUpdateRouter.stableId("a"), LiveUpdateRouter.stableId("b"))
    }
}
