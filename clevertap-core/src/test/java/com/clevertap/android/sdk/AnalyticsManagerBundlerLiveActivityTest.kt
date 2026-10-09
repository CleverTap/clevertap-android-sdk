package com.clevertap.android.sdk

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Covers the Live Activity lifecycle-event JSON shape (evtName + state inside evtData). */
@RunWith(RobolectricTestRunner::class)
class AnalyticsManagerBundlerLiveActivityTest {

    @Test
    fun `liveActivityEventJson wraps root + state under the Live Activity event`() {
        val root = JSONObject().put("wzrk_id", "abc").put("wzrk_activityId", "act-1")

        val event = AnalyticsManagerBundler.liveActivityEventJson(root, Constants.LIVE_ACTIVITY_STATE_STARTED)

        assertEquals(Constants.LIVE_ACTIVITY_EVENT_NAME, event.getString("evtName"))
        val evtData = event.getJSONObject("evtData")
        assertEquals(Constants.LIVE_ACTIVITY_STATE_STARTED, evtData.getString(Constants.LIVE_ACTIVITY_STATE_KEY))
        // the original wzrk_ keys are preserved in evtData
        assertEquals("abc", evtData.getString("wzrk_id"))
        assertEquals("act-1", evtData.getString("wzrk_activityId"))
    }
}
