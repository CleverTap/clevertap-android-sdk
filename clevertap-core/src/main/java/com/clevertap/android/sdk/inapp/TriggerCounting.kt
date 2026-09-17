package com.clevertap.android.sdk.inapp

/**
 * Read-only view of per-campaign trigger counts. Lets limit matching run against a virtual count
 * without touching the live store — the basis of the non-mutating dry-run evaluation used by
 * Option-2 arbitration prediction (SDK-6143 / SDK-6144).
 *
 * [TriggerManager] is the live, mutating implementation; [OffsetTriggerCounter] wraps it to report
 * `count + offset` without writing.
 */
internal interface TriggerCounting {

    /** @return the trigger count for [campaignId], or 0 if none recorded. */
    fun getTriggers(campaignId: String): Int
}
