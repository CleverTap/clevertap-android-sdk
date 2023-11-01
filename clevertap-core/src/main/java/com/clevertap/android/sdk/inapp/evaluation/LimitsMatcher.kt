package com.clevertap.android.sdk.inapp.evaluation

import com.clevertap.android.sdk.inapp.ImpressionManager
import com.clevertap.android.sdk.inapp.TriggerManager
import com.clevertap.android.sdk.response.data.WhenLimit

/**
 * A utility class responsible for matching limits against campaign conditions.
 */
class LimitsMatcher(
    private val manager: ImpressionManager,
    private val triggerManager: TriggerManager
) {

    /**
     * Checks if all given limits specified by JSON objects are met for a campaign.
     *
     * @param whenLimits The list of JSON objects representing the limits to be checked.
     * @param campaignId The unique identifier of the campaign.
     * @param manager The ImpressionManager used to track campaign impressions.
     * @return `true` if all limits are met, otherwise `false`.
     */
    fun matchWhenLimits(
        whenLimits: List<WhenLimit>,
        campaignId: String,
    ): Boolean = whenLimits.all {
        matchLimit(it, campaignId)
    }

    /**
     * Checks if a single limit specified by the [limit] adapter is met for a campaign.
     *
     * @param limit The adapter representing the limit condition.
     * @param campaignId The unique identifier of the campaign.
     * @param manager The ImpressionManager used to track campaign impressions.
     * @return `true` if the limit is met, otherwise `false`.
     */
    private fun matchLimit(
        limit: WhenLimit,
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

}
