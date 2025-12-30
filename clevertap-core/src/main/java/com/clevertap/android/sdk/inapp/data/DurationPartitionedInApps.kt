package com.clevertap.android.sdk.inapp.data

import com.clevertap.android.sdk.inapp.data.DurationPartitionedInApps.ImmediateAndDelayed
import com.clevertap.android.sdk.inapp.data.DurationPartitionedInApps.ImmediateDelayedAndInAction
import com.clevertap.android.sdk.inapp.data.DurationPartitionedInApps.InActionOnly
import com.clevertap.android.sdk.inapp.data.DurationPartitionedInApps.UnknownAndInAction
import org.json.JSONArray

/**
 * Sealed class hierarchy representing in-app notifications partitioned by their display duration.
 *
 * Duration types:
 * - **Immediate**: No delay, display right away
 * - **Delayed**: Has `delayAfterTrigger`, display after specified seconds
 * - **InAction**: Has `inactionDuration`, fetch content after user inactivity timer
 * - **Unknown**: Actual duration (immediate/delayed) determined later via eval flow
 *
 * Subclasses:
 * - [ImmediateAndDelayed]: For Client-Side and AppLaunch Server-Side in-apps
 * - [UnknownAndInAction]: For Server-Side metadata in-apps
 * - [ImmediateDelayedAndInAction]: For Legacy in-apps
 * - [InActionOnly]: For AppLaunch Server-Side metadata in-apps
 */
sealed class DurationPartitionedInApps {

    /**
     * Partition containing immediate and delayed duration in-apps.
     *
     * Used for:
     * - Client-Side in-apps (`inapp_notifs_cs`)
     * - AppLaunch Server-Side in-apps (`inapp_notifs_applaunched`)
     *
     * Note: These sources do NOT support `inactionDuration`.
     */
    data class ImmediateAndDelayed(
        val immediateInApps: JSONArray,
        val delayedInApps: JSONArray
    ) : DurationPartitionedInApps() {

        fun hasImmediateInApps(): Boolean = immediateInApps.length() > 0

        fun hasDelayedInApps(): Boolean = delayedInApps.length() > 0

        companion object {
            fun empty() = ImmediateAndDelayed(JSONArray(), JSONArray())
        }
    }

    /**
     * Partition containing unknown and inAction duration in-apps.
     *
     * Used for:
     * - Server-Side metadata in-apps (`inapp_notifs_ss`)
     *
     * - **Unknown**: No `inactionDuration` → goes through `inApps_eval` flow
     *   → actual duration (immediate/delayed) determined after eval response
     * - **InAction**: Has `inactionDuration` → fetch content after inactivity timer
     */
    data class UnknownAndInAction(
        val unknownDurationInApps: JSONArray,
        val inActionInApps: JSONArray
    ) : DurationPartitionedInApps() {

        fun hasUnknownDurationInApps(): Boolean = unknownDurationInApps.length() > 0

        fun hasInActionInApps(): Boolean = inActionInApps.length() > 0

        companion object {
            fun empty() = UnknownAndInAction(JSONArray(), JSONArray())
        }
    }

    /**
     * Partition containing only inAction duration in-apps.
     *
     * Used for:
     * - AppLaunch Server-Side metadata in-apps (`inapp_notifs_applaunched_meta`)
     *
     * All items have `inactionDuration` → fetch content after inactivity timer
     */
    data class InActionOnly(
        val inActionInApps: JSONArray
    ) : DurationPartitionedInApps() {

        fun hasInActionInApps(): Boolean = inActionInApps.length() > 0

        companion object {
            fun empty() = InActionOnly(JSONArray())
        }
    }

    /**
     * Partition containing all duration types: immediate, delayed, and inAction.
     *
     * Used for:
     * - Legacy in-apps (`inapp_notifs`)
     *
     * Rules:
     * - An in-app can have EITHER `delayAfterTrigger` OR `inactionDuration`, never both
     * - `delayAfterTrigger` always comes WITH in-app content
     * - `inactionDuration` always comes WITHOUT content (content fetched after timer)
     */
    data class ImmediateDelayedAndInAction(
        val immediateInApps: JSONArray,
        val delayedInApps: JSONArray,
        val inActionInApps: JSONArray
    ) : DurationPartitionedInApps() {

        fun hasImmediateInApps(): Boolean = immediateInApps.length() > 0

        fun hasDelayedInApps(): Boolean = delayedInApps.length() > 0

        fun hasInActionInApps(): Boolean = inActionInApps.length() > 0

        companion object {
            fun empty() = ImmediateDelayedAndInAction(JSONArray(), JSONArray(), JSONArray())
        }
    }
}