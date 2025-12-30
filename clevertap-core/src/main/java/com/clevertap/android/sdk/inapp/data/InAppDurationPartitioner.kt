package com.clevertap.android.sdk.inapp.data

import com.clevertap.android.sdk.inapp.data.DurationPartitionedInApps.ImmediateAndDelayed
import com.clevertap.android.sdk.inapp.data.DurationPartitionedInApps.ImmediateDelayedAndInAction
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
import com.clevertap.android.sdk.inapp.data.InAppDurationPartitioner.partitionServerSideMetaInApps
import com.clevertap.android.sdk.inapp.data.InAppInActionConstants.INAPP_DEFAULT_INACTION_SECONDS
import com.clevertap.android.sdk.inapp.data.InAppInActionConstants.INAPP_INACTION_DURATION
import com.clevertap.android.sdk.inapp.data.InAppInActionConstants.INAPP_MAX_INACTION_SECONDS
import com.clevertap.android.sdk.inapp.data.InAppInActionConstants.INAPP_MIN_INACTION_SECONDS
import com.clevertap.android.sdk.iterator
import org.json.JSONArray
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
 * - [partitionLegacyInApps]: immediate + delayed + inAction
 * - [partitionClientSideInApps]: immediate + delayed
 * - [partitionServerSideMetaInApps]: unknown + inAction
 * - [partitionAppLaunchServerSideInApps]: immediate + delayed
 * - [partitionAppLaunchServerSideMetaInApps]: inAction only
 */
internal object InAppDurationPartitioner {

    /**
     * Partitions legacy in-apps by duration: immediate, delayed, and inAction.
     *
     * Rules:
     * - An in-app can have EITHER `delayAfterTrigger` OR `inactionDuration`, never both
     * - `delayAfterTrigger` always comes with in-app content
     * - `inactionDuration` always comes without content (needs fetch after timer)
     *
     * @param inAppsArray The JSON array of legacy in-app notifications
     * @return [ImmediateDelayedAndInAction] containing partitioned in-apps
     */
    fun partitionLegacyInApps(inAppsArray: JSONArray?): ImmediateDelayedAndInAction {
        if (inAppsArray == null) {
            return ImmediateDelayedAndInAction.empty()
        }

        val immediate = mutableListOf<JSONObject>()
        val delayed = mutableListOf<JSONObject>()
        val inAction = mutableListOf<JSONObject>()

        inAppsArray.iterator<JSONObject> { inApp ->
            when {
                hasInActionDuration(inApp) -> inAction.add(inApp)
                hasDelayedDuration(inApp) -> delayed.add(inApp)
                else -> immediate.add(inApp)
            }
        }

        return ImmediateDelayedAndInAction(
            immediateInApps = JSONArray(immediate),
            delayedInApps = JSONArray(delayed),
            inActionInApps = JSONArray(inAction)
        )
    }

    /**
     * Partitions client-side in-apps by duration: immediate and delayed.
     *
     * Note: Client-side does NOT support `inactionDuration`.
     *
     * @param inAppsArray The JSON array of client-side in-app notifications
     * @return [ImmediateAndDelayed] containing partitioned in-apps
     */
    fun partitionClientSideInApps(inAppsArray: JSONArray?): ImmediateAndDelayed {
        if (inAppsArray == null) {
            return ImmediateAndDelayed.empty()
        }

        val immediate = mutableListOf<JSONObject>()
        val delayed = mutableListOf<JSONObject>()

        inAppsArray.iterator<JSONObject> { inApp ->
            when {
                hasDelayedDuration(inApp) -> delayed.add(inApp)
                else -> immediate.add(inApp)
            }
        }

        return ImmediateAndDelayed(
            immediateInApps = JSONArray(immediate),
            delayedInApps = JSONArray(delayed)
        )
    }

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
     * @param inAppsArray The JSON array of server-side metadata in-app notifications
     * @return [UnknownAndInAction] containing partitioned in-apps
     */
    fun partitionServerSideMetaInApps(inAppsArray: JSONArray?): UnknownAndInAction {
        if (inAppsArray == null) {
            return UnknownAndInAction.empty()
        }

        val unknownDuration = mutableListOf<JSONObject>()
        val inAction = mutableListOf<JSONObject>()

        inAppsArray.iterator<JSONObject> { inApp ->
            when {
                hasInActionDuration(inApp) -> inAction.add(inApp)
                else -> unknownDuration.add(inApp)
            }
        }

        return UnknownAndInAction(
            unknownDurationInApps = JSONArray(unknownDuration),
            inActionInApps = JSONArray(inAction)
        )
    }

    /**
     * Partitions app-launch server-side in-apps by duration: immediate and delayed.
     *
     * Note: This source does NOT support `inactionDuration`.
     * InAction items come separately in `inapp_notifs_applaunched_meta`.
     *
     * @param inAppsArray The JSON array of app-launch server-side in-app notifications
     * @return [ImmediateAndDelayed] containing partitioned in-apps
     */
    fun partitionAppLaunchServerSideInApps(inAppsArray: JSONArray?): ImmediateAndDelayed {
        return partitionClientSideInApps(inAppsArray)
    }

    /**
     * Wraps app-launch server-side metadata in-apps as inAction only.
     *
     * All items in `inapp_notifs_applaunched_meta` have `inactionDuration`
     * → fetch content after inactivity timer.
     *
     * Note: No partitioning needed as all items are inAction.
     *
     * @param inAppsArray The JSON array of app-launch server-side metadata in-app notifications
     * @return [InActionOnly] containing inAction in-apps
     */
    fun partitionAppLaunchServerSideMetaInApps(inAppsArray: JSONArray?): InActionOnly {
        return InActionOnly(inAppsArray ?: JSONArray())
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