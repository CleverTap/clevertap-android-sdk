package com.clevertap.android.sdk.inapp.data

import com.clevertap.android.sdk.inapp.data.DurationPartitionedInApps.ImmediateAndDelayed
import com.clevertap.android.sdk.inapp.data.DurationPartitionedInApps.InActionOnly
import com.clevertap.android.sdk.inapp.data.DurationPartitionedInApps.UnknownAndInAction
import org.json.JSONArray
import org.json.JSONObject

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
 * - [ImmediateAndDelayed]: For Legacy, Client-Side, and AppLaunch Server-Side in-apps
 * - [UnknownAndInAction]: For Server-Side metadata in-apps
 * - [InActionOnly]: For Legacy metadata and AppLaunch Server-Side metadata in-apps
 */
sealed class DurationPartitionedInApps {

    /**
     * Partition containing immediate and delayed duration in-apps.
     *
     * Used for:
     * - Legacy in-apps (`inapp_notifs`)
     * - Client-Side in-apps (`inapp_notifs_cs`)
     * - AppLaunch Server-Side in-apps (`inapp_notifs_applaunched`)
     *
     * Note: These sources do NOT contain `inactionDuration` items.
     * InAction items come separately in their respective `_meta` keys.
     */
    data class ImmediateAndDelayed(
        val immediateInApps: List<JSONObject>,
        val delayedInApps: List<JSONObject>
    ) : DurationPartitionedInApps() {

        fun hasImmediateInApps(): Boolean = immediateInApps.isNotEmpty()

        fun hasDelayedInApps(): Boolean = delayedInApps.isNotEmpty()

        companion object {
            fun empty() = ImmediateAndDelayed(emptyList(), emptyList())
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
        val unknownDurationInApps: List<JSONObject>,
        val inActionInApps: List<JSONObject>
    ) : DurationPartitionedInApps() {

        fun hasUnknownDurationInApps(): Boolean = unknownDurationInApps.isNotEmpty()

        fun hasInActionInApps(): Boolean = inActionInApps.isNotEmpty()

        companion object {
            fun empty() = UnknownAndInAction(emptyList(), emptyList())
        }
    }

    /**
     * Partition containing only inAction duration in-apps.
     *
     * Used for:
     * - Legacy metadata in-apps (`inapp_notifs_meta`)
     * - AppLaunch Server-Side metadata in-apps (`inapp_notifs_applaunched_meta`)
     *
     * All items have `inactionDuration` → fetch content after inactivity timer.
     */
    data class InActionOnly(
        val inActionInApps: List<JSONObject>
    ) : DurationPartitionedInApps() {

        fun hasInActionInApps(): Boolean = inActionInApps.isNotEmpty()

        companion object {
            fun empty() = InActionOnly(emptyList())
        }
    }
}