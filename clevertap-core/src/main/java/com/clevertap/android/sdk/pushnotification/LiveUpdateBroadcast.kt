package com.clevertap.android.sdk.pushnotification

import android.os.Bundle
import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.Constants

/**
 * Pure payload reads for a broadcast-mode Live Update. Broadcast START arrives 1:1 on the device
 * token like any Live Update, but carries `wzrk_activityType = "1"` + the `wzrk_topic` on which the
 * run's later updates/end are delivered.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object LiveUpdateBroadcast {

    /** True for a Live Update whose `wzrk_activityType` is "1". */
    @JvmStatic
    fun isBroadcast(extras: Bundle?): Boolean =
        LiveUpdateRouter.isLiveUpdate(extras) &&
            Constants.LIVE_UPDATE_ACTIVITY_TYPE_BROADCAST == extras?.getString(Constants.WZRK_LIVE_UPDATE_ACTIVITY_TYPE)

    @JvmStatic
    fun topic(extras: Bundle?): String? =
        extras?.getString(Constants.WZRK_LIVE_UPDATE_TOPIC)?.takeIf { it.isNotBlank() }

    @JvmStatic
    fun isStart(extras: Bundle?): Boolean =
        Constants.WZRK_LIVE_UPDATE_EVENT_START.equals(
            extras?.getString(Constants.WZRK_LIVE_UPDATE_EVENT), ignoreCase = true
        )

    @JvmStatic
    fun isEnd(extras: Bundle?): Boolean =
        Constants.WZRK_LIVE_UPDATE_EVENT_END.equals(
            extras?.getString(Constants.WZRK_LIVE_UPDATE_EVENT), ignoreCase = true
        )

    /**
     * Epoch seconds at which the Live Update must be removed. `dismissal_date` when valid, else the
     * run's START (`wzrk_runId`, else `timestamp`, else [nowSeconds]) + 12h.
     */
    @JvmStatic
    fun expiresAtSeconds(extras: Bundle?, nowSeconds: Long): Long {
        extras?.getString(Constants.WZRK_LIVE_UPDATE_DISMISSAL_DATE)?.toLongOrNull()
            ?.takeIf { it > 0 }
            ?.let { return it }
        val start = extras?.getString(Constants.WZRK_LIVE_UPDATE_RUN_ID)?.toLongOrNull()
            ?: extras?.getString("timestamp")?.toLongOrNull()
            ?: nowSeconds
        return start + Constants.LIVE_UPDATE_DEFAULT_LIFETIME_SECONDS
    }
}
