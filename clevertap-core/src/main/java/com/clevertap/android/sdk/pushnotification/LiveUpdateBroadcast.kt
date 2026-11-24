package com.clevertap.android.sdk.pushnotification

import android.os.Bundle
import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.Constants

/**
 * Pure payload reads for broadcast topic subscription control. Deliberately independent of the Live
 * Activity lifecycle keys (`wzrk_la`, `wzrk_la_event`): subscription is driven only by
 * `wzrk_topic_action` + `wzrk_topic`, so the two concerns can share a push or travel separately.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object LiveUpdateBroadcast {

    @JvmStatic
    fun topic(extras: Bundle?): String? =
        extras?.getString(Constants.WZRK_LIVE_UPDATE_TOPIC)?.takeIf { it.isNotBlank() }

    @JvmStatic
    fun isSubscribe(extras: Bundle?): Boolean =
        Constants.TOPIC_ACTION_SUBSCRIBE.equals(extras?.getString(Constants.WZRK_TOPIC_ACTION), ignoreCase = true)

    @JvmStatic
    fun isUnsubscribe(extras: Bundle?): Boolean =
        Constants.TOPIC_ACTION_UNSUBSCRIBE.equals(extras?.getString(Constants.WZRK_TOPIC_ACTION), ignoreCase = true)

    @JvmStatic
    fun unsubscribeOnDismiss(extras: Bundle?): Boolean =
        "true".equals(extras?.getString(Constants.WZRK_TOPIC_UNSUB_ON_DISMISS), ignoreCase = true)

    /**
     * Epoch seconds at which the subscription expires. `dismissal_date` when valid, else the run's
     * START (`wzrk_runId`, else `timestamp`, else [nowSeconds]) + 12h.
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
