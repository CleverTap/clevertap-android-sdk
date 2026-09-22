package com.clevertap.android.sdk.inapp

/**
 * A [TriggerCounting] that reports the live count plus a fixed [offset], without writing anything.
 *
 * The real [evaluate][com.clevertap.android.sdk.inapp.evaluation.EvaluationManager] increments a
 * campaign's trigger count *before* checking its limits, so limits see `count + 1`. The dry-run
 * evaluation must reproduce that view without the increment (evaluating speculatively and again for
 * real would double-count "show once per N triggers" rules). Wrapping the live counter with
 * `offset = 1` gives exactly the same value the real path would have matched against.
 */
internal class OffsetTriggerCounter(
    private val delegate: TriggerCounting,
    private val offset: Int = 1
) : TriggerCounting {

    override fun getTriggers(campaignId: String): Int = delegate.getTriggers(campaignId) + offset
}
