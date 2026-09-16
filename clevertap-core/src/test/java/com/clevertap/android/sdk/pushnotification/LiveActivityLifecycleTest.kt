package com.clevertap.android.sdk.pushnotification

import com.clevertap.android.sdk.Constants
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure-JVM unit tests for the BE-driven wzrk_la_event -> lifecycle-state mapping. */
class LiveActivityLifecycleTest {

    @Test
    fun `start maps to Started`() {
        assertEquals(Constants.LIVE_ACTIVITY_STATE_STARTED, LiveActivityLifecycle.state("start"))
    }

    @Test
    fun `update maps to Updated`() {
        assertEquals(Constants.LIVE_ACTIVITY_STATE_UPDATED, LiveActivityLifecycle.state("update"))
    }

    @Test
    fun `end maps to Ended`() {
        assertEquals(Constants.LIVE_ACTIVITY_STATE_ENDED, LiveActivityLifecycle.state("end"))
    }

    @Test
    fun `mapping is case-insensitive`() {
        assertEquals(Constants.LIVE_ACTIVITY_STATE_STARTED, LiveActivityLifecycle.state("START"))
        assertEquals(Constants.LIVE_ACTIVITY_STATE_ENDED, LiveActivityLifecycle.state("End"))
    }

    @Test
    fun `null or unknown event defaults to Updated`() {
        assertEquals(Constants.LIVE_ACTIVITY_STATE_UPDATED, LiveActivityLifecycle.state(null))
        assertEquals(Constants.LIVE_ACTIVITY_STATE_UPDATED, LiveActivityLifecycle.state(""))
        assertEquals(Constants.LIVE_ACTIVITY_STATE_UPDATED, LiveActivityLifecycle.state("whatever"))
    }
}
