package com.clevertap.android.sdk.pushnotification

import com.clevertap.android.sdk.Constants
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure-JVM unit tests for the BE-driven wzrk_la_event -> lifecycle-state mapping. */
class LiveUpdateLifecycleTest {

    @Test
    fun `start maps to Started`() {
        assertEquals(Constants.LIVE_UPDATE_STATE_STARTED, LiveUpdateLifecycle.state("start"))
    }

    @Test
    fun `update maps to Updated`() {
        assertEquals(Constants.LIVE_UPDATE_STATE_UPDATED, LiveUpdateLifecycle.state("update"))
    }

    @Test
    fun `end maps to Ended`() {
        assertEquals(Constants.LIVE_UPDATE_STATE_ENDED, LiveUpdateLifecycle.state("end"))
    }

    @Test
    fun `mapping is case-insensitive`() {
        assertEquals(Constants.LIVE_UPDATE_STATE_STARTED, LiveUpdateLifecycle.state("START"))
        assertEquals(Constants.LIVE_UPDATE_STATE_ENDED, LiveUpdateLifecycle.state("End"))
    }

    @Test
    fun `null or unknown event defaults to Updated`() {
        assertEquals(Constants.LIVE_UPDATE_STATE_UPDATED, LiveUpdateLifecycle.state(null))
        assertEquals(Constants.LIVE_UPDATE_STATE_UPDATED, LiveUpdateLifecycle.state(""))
        assertEquals(Constants.LIVE_UPDATE_STATE_UPDATED, LiveUpdateLifecycle.state("whatever"))
    }
}
