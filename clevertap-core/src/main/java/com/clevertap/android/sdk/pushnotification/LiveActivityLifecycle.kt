package com.clevertap.android.sdk.pushnotification

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.Constants

/**
 * Pure mapping from a Live Update render to its lifecycle state, extracted from
 * `PushProviders.postNotificationRendered` so the Started/Updated/Ended transition is
 * unit-testable in isolation (the "is this the first render?" side-effect stays in the caller,
 * behind the push-id dedup store).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object LiveActivityLifecycle {

    /**
     * @param isEnd         true when the push carries `wzrk_la_event=end` (terminal).
     * @param isFirstRender true the first time this activity id is rendered.
     */
    @JvmStatic
    fun state(isEnd: Boolean, isFirstRender: Boolean): String = when {
        isEnd -> Constants.LIVE_ACTIVITY_STATE_ENDED
        isFirstRender -> Constants.LIVE_ACTIVITY_STATE_STARTED
        else -> Constants.LIVE_ACTIVITY_STATE_UPDATED
    }
}
