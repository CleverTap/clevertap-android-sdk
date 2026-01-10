package com.clevertap.android.sdk.inapp.data

import com.clevertap.android.sdk.inapp.data.DurationPartitionedInApps.ImmediateAndDelayed
import com.clevertap.android.sdk.inapp.data.DurationPartitionedInApps.InActionOnly
import com.clevertap.android.sdk.inapp.data.DurationPartitionedInApps.UnknownAndInAction
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_DEFAULT_DELAY_SECONDS
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_DELAY_AFTER_TRIGGER
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_MAX_DELAY_SECONDS
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants.INAPP_MIN_DELAY_SECONDS
import com.clevertap.android.sdk.inapp.data.InAppDurationPartitioner.partitionAppLaunchServerSideInApps
import com.clevertap.android.sdk.inapp.data.InAppDurationPartitioner.partitionAppLaunchServerSideMetaInApps
import com.clevertap.android.sdk.inapp.data.InAppDurationPartitioner.partitionClientSideInApps
import com.clevertap.android.sdk.inapp.data.InAppDurationPartitioner.partitionLegacyInApps
import com.clevertap.android.sdk.inapp.data.InAppDurationPartitioner.partitionLegacyMetaInApps
import com.clevertap.android.sdk.inapp.data.InAppDurationPartitioner.partitionServerSideMetaInApps
import com.clevertap.android.sdk.inapp.data.InAppInActionConstants.INAPP_DEFAULT_INACTION_SECONDS
import com.clevertap.android.sdk.inapp.data.InAppInActionConstants.INAPP_INACTION_DURATION
import com.clevertap.android.sdk.inapp.data.InAppInActionConstants.INAPP_MAX_INACTION_SECONDS
import com.clevertap.android.sdk.inapp.data.InAppInActionConstants.INAPP_MIN_INACTION_SECONDS
import org.json.JSONObject

/**
 * Utility object for partitioning in-app notifications by their display duration.
 *
 * Duration categories:
 * - **Immediate**: duration = 0, no delay fields present
 * - **Delayed**: has valid `delayAfterTrigger` (1-1200 seconds)
 * - **InAction**: has valid `inactionDuration` (1-1200 seconds)
 * - **Unknown**: actual duration determined later via eval flow (used for SS meta)
 *
 * Provides specialized partition functions for different in-app sources:
 * - [partitionLegacyInApps]: immediate + delayed
 * - [partitionLegacyMetaInApps]: inAction only
 * - [partitionClientSideInApps]: immediate + delayed
 * - [partitionServerSideMetaInApps]: unknown + inAction
 * - [partitionAppLaunchServerSideInApps]: immediate + delayed
 * - [partitionAppLaunchServerSideMetaInApps]: inAction only
 */
internal object InAppDurationPartitioner {

    /**
     * Partitions legacy in-apps by duration: immediate and delayed.
     *
     * Note: This source does NOT contain `inactionDuration` items.
     * InAction items come separately in `inapp_notifs_meta`.
     *
     * @param inAppsList The list of legacy in-app notifications
     * @return [ImmediateAndDelayed] containing partitioned in-apps
     */
    fun partitionLegacyInApps(inAppsList: List<JSONObject>): ImmediateAndDelayed =
        partitionByDelay(inAppsList)

    /**
     * Wraps legacy metadata in-apps as inAction only.
     *
     * All items in `inapp_notifs_meta` have `inactionDuration`
     * → fetch content after inactivity timer.
     *
     * Note: No partitioning needed as all items are inAction.
     *
     * @param inAppsList The list of legacy metadata in-app notifications
     * @return [InActionOnly] containing inAction in-apps
     */
    fun partitionLegacyMetaInApps(inAppsList: List<JSONObject>): InActionOnly =
        InActionOnly(inAppsList)

    /**
     * Partitions client-side in-apps by duration: immediate and delayed.
     *
     * Note: Client-side does NOT support `inactionDuration`.
     *
     * @param inAppsList The list of client-side in-app notifications
     * @return [ImmediateAndDelayed] containing partitioned in-apps
     */
    fun partitionClientSideInApps(inAppsList: List<JSONObject>): ImmediateAndDelayed =
        partitionByDelay(inAppsList)

    /**
     * Partitions server-side metadata in-apps by duration: unknown and inAction.
     *
     * - **Unknown**: No `inactionDuration` → goes through `inApps_eval` flow
     *   → actual duration (immediate/delayed) determined after eval response
     * - **InAction**: Has `inactionDuration` → fetch content after inactivity timer
     *
     * Note: Server-side meta does NOT have `delayAfterTrigger` directly
     * (delay info comes only after eval or inAction fetch).
     *
     * @param inAppsList The list of server-side metadata in-app notifications
     * @return [UnknownAndInAction] containing partitioned in-apps
     */
    fun partitionServerSideMetaInApps(inAppsList: List<JSONObject>): UnknownAndInAction {
        if (inAppsList.isEmpty()) return UnknownAndInAction.empty()

        val (inAction, unknown) = inAppsList.partition { hasInActionDuration(it) }
        return UnknownAndInAction(
            unknownDurationInApps = unknown,
            inActionInApps = inAction
        )
    }

    /**
     * Partitions app-launch server-side in-apps by duration: immediate and delayed.
     *
     * Note: This source does NOT support `inactionDuration`.
     * InAction items come separately in `inapp_notifs_applaunched_meta`.
     *
     * @param inAppsList The list of app-launch server-side in-app notifications
     * @return [ImmediateAndDelayed] containing partitioned in-apps
     */
    fun partitionAppLaunchServerSideInApps(inAppsList: List<JSONObject>): ImmediateAndDelayed =
        partitionByDelay(inAppsList)

    /**
     * Wraps app-launch server-side metadata in-apps as inAction only.
     *
     * All items in `inapp_notifs_applaunched_meta` have `inactionDuration`
     * → fetch content after inactivity timer.
     *
     * Note: No partitioning needed as all items are inAction.
     *
     * @param inAppsList The list of app-launch server-side metadata in-app notifications
     * @return [InActionOnly] containing inAction in-apps
     */
    fun partitionAppLaunchServerSideMetaInApps(inAppsList: List<JSONObject>): InActionOnly =
        InActionOnly(inAppsList)

    // ==================== Private Helpers ====================

    /**
     * Common partitioning logic for immediate/delayed in-apps.
     * Used by legacy, client-side, and app-launch server-side sources.
     */
    private fun partitionByDelay(inAppsList: List<JSONObject>): ImmediateAndDelayed {
        if (inAppsList.isEmpty()) return ImmediateAndDelayed.empty()

        val (delayed, immediate) = inAppsList.partition { hasDelayedDuration(it) }
        return ImmediateAndDelayed(
            immediateInApps = immediate,
            delayedInApps = delayed
        )
    }

    /**
     * Checks if the in-app has a valid inAction duration.
     * Valid range: 1-1200 seconds
     */
    private fun hasInActionDuration(inApp: JSONObject): Boolean {
        val inactionSeconds = inApp.optInt(INAPP_INACTION_DURATION, INAPP_DEFAULT_INACTION_SECONDS)
        return inactionSeconds in INAPP_MIN_INACTION_SECONDS..INAPP_MAX_INACTION_SECONDS
    }

    /**
     * Checks if the in-app has a valid delayed duration.
     * Valid range: 1-1200 seconds
     */
    private fun hasDelayedDuration(inApp: JSONObject): Boolean {
        val delaySeconds = inApp.optInt(INAPP_DELAY_AFTER_TRIGGER, INAPP_DEFAULT_DELAY_SECONDS)
        return delaySeconds in INAPP_MIN_DELAY_SECONDS..INAPP_MAX_DELAY_SECONDS
    }
}