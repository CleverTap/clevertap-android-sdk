package com.clevertap.android.sdk.pushnotification

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.Constants

/**
 * Pure mapping from a Live Update's `wzrk_la_event` (sent by BE) to its analytics lifecycle state.
 * The event is authoritative — the SDK no longer infers Started vs Updated by tracking first-render,
 * so there is no local/DB state (matches iOS ActivityKit, where start vs update is explicit).
 *
 * `start` → Started, `update` → Updated, `end` → Ended; anything absent/unknown → Updated (an
 * in-place render is, by default, an update).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object LiveActivityLifecycle {

    @JvmStatic
    fun state(event: String?): String = when {
        Constants.WZRK_LIVE_ACTIVITY_EVENT_START.equals(event, ignoreCase = true) ->
            Constants.LIVE_ACTIVITY_STATE_STARTED

        Constants.WZRK_LIVE_ACTIVITY_EVENT_END.equals(event, ignoreCase = true) ->
            Constants.LIVE_ACTIVITY_STATE_ENDED

        else -> Constants.LIVE_ACTIVITY_STATE_UPDATED
    }
}
