package com.clevertap.android.sdk.pushnotification

import com.clevertap.android.sdk.Constants
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure-JVM unit tests for the Live Update lifecycle-state mapping. */
class LiveActivityLifecycleTest {

    @Test
    fun `end event maps to Ended`() {
        assertEquals(Constants.LIVE_ACTIVITY_STATE_ENDED, LiveActivityLifecycle.state(isEnd = true, isFirstRender = false))
    }

    @Test
    fun `end wins even if it were the first render`() {
        assertEquals(Constants.LIVE_ACTIVITY_STATE_ENDED, LiveActivityLifecycle.state(isEnd = true, isFirstRender = true))
    }

    @Test
    fun `first render maps to Started`() {
        assertEquals(Constants.LIVE_ACTIVITY_STATE_STARTED, LiveActivityLifecycle.state(isEnd = false, isFirstRender = true))
    }

    @Test
    fun `subsequent render maps to Updated`() {
        assertEquals(Constants.LIVE_ACTIVITY_STATE_UPDATED, LiveActivityLifecycle.state(isEnd = false, isFirstRender = false))
    }
}
