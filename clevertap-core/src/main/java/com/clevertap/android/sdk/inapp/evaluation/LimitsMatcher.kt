package com.clevertap.android.sdk.inapp.evaluation

import com.clevertap.android.sdk.inapp.ImpressionManager
import com.clevertap.android.sdk.inapp.TriggerManager

/**
 * The `LimitsMatcher` class is responsible for evaluating limits defined by [LimitAdapter]s for a given campaign.
 * It checks whether the specified limits are met and provides functionality to determine if an impression should
 * be discarded based on certain conditions.
 *
 * @property manager The [ImpressionManager] used to track campaign impressions.
 * @property triggerManager The [TriggerManager] used to manage triggers for in-app notifications.
 */
internal class LimitsMatcher(
    private val manager: ImpressionManager,
    private val triggerManager: TriggerManager
) {

    /**
     * Checks if all given limits specified by [LimitAdapter]s are met for a campaign.
     *
     * @param whenLimits The list of [LimitAdapter] representing the limits to be checked.
     * @param campaignId The unique identifier of the campaign.
     * @return `true` if all limits are met, otherwise `false`.
     */
    fun matchWhenLimits(
        whenLimits: List<LimitAdapter>,
        campaignId: String,
    ): Boolean = whenLimits.all {
        matchLimit(it, campaignId)
    }

    /**
     * Checks if a single limit specified by the [limit] adapter is met for a campaign.
     *
     * @param limit The adapter representing the limit condition.
     * @param campaignId The unique identifier of the campaign.
     * @return `true` if the limit is met, otherwise `false`.
     */
    private fun matchLimit(
        limit: LimitAdapter,
        campaignId: String
    ): Boolean {
        return when (limit.limitType) {
            LimitType.Session -> manager.perSession(campaignId) < limit.limit
            LimitType.Seconds -> manager.perSecond(campaignId, limit.frequency) < limit.limit
            LimitType.Minutes -> manager.perMinute(campaignId, limit.frequency) < limit.limit
            LimitType.Hours -> manager.perHour(campaignId, limit.frequency) < limit.limit
            LimitType.Days -> manager.perDay(campaignId, limit.frequency) < limit.limit
            LimitType.Weeks -> manager.perWeek(campaignId, limit.frequency) < limit.limit
            LimitType.Ever -> manager.getImpressions(campaignId).size < limit.limit
            LimitType.OnEvery -> {
                val triggerCount = triggerManager.getTriggers(campaignId)
                /*TODO: VERIFY IF WE NEED TO ADD 1 TO TRIGGER COUNT, IF THE IMPRESSION HAS BEEN
                   ALREADY RECORDED FROM ELSEWHERE
                val tc = triggerCount + 1
                tc % limit.limit == 0*/
                triggerCount % limit.limit == 0
            }

            LimitType.OnExactly -> {
                val triggerCount = triggerManager.getTriggers(campaignId)
                /*TODO: VERIFY IF WE NEED TO ADD 1 TO TRIGGER COUNT, IF THE IMPRESSION HAS BEEN
                    ALREADY RECORDED FROM ELSEWHERE
                 val tc = triggerCount + 1
                tc == limit.limit*/
                triggerCount == limit.limit
            }
        }
    }

    fun shouldDiscard(
        whenLimits: List<LimitAdapter>,
        campaignId: String,
    ): Boolean {
        var discard = false
        whenLimits.forEach { limitAdapter ->
            discard = discard || when (limitAdapter.limitType) {
                LimitType.Ever -> {
                    !matchLimit(limitAdapter, campaignId)
                }
                else -> {
                    false
                }
            }
        }
        return discard
    }

}
