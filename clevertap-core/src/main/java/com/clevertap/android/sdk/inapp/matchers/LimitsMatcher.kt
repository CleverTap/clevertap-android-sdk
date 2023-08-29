package com.clevertap.android.sdk.inapp.matchers

import com.clevertap.android.sdk.inapp.ImpressionManager
import org.json.JSONArray
import org.json.JSONObject

class LimitsMatcher {

  fun match(
    whenLimits: JSONArray,
    campaignId: String,
    impressionManager: ImpressionManager
  ): Boolean {
    // elements in array are AND-ed
    // currently only one onEvery or onExactly would be presented
    for (i in 0 until whenLimits.length()) {
      val limit = LimitAdapter(whenLimits[i] as JSONObject)
      val matched = match(limit, campaignId, impressionManager)
      if (!matched) {
        return false
      }
    }
    return true
  }

  private fun match(
    limit: LimitAdapter,
    campaignId: String,
    impressionManager: ImpressionManager
  ): Boolean {
    when (limit.getType()) {
      LimitType.Session -> {
        if (impressionManager.perSession(campaignId) < limit.getLimit()) {
          return true
        }
      }
      else -> Unit // TODO implement all remaining cases and remove `else` clause
    }
    return false
  }

}
