package com.clevertap.android.sdk.inapp.evaluation

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.isNotNullAndEmpty
import com.clevertap.android.sdk.orEmptyArray
import com.clevertap.android.sdk.toList
import org.json.JSONObject

/**
 * Channel-agnostic parsing of a campaign's `whenTriggers` / `whenLimits` blocks.
 *
 * The trigger/limit schema is identical across channels (in-app + Native Display), so both
 * evaluators share this instead of keeping verbatim copies (prototype A — SDK-6055 design review).
 */
internal object EvalRules {

    fun whenTriggers(triggerJson: JSONObject): List<TriggerAdapter> {
        val whenTriggers = triggerJson.optJSONArray(Constants.INAPP_WHEN_TRIGGERS).orEmptyArray()
        return (0 until whenTriggers.length()).mapNotNull {
            (whenTriggers[it] as? JSONObject)?.let { obj -> TriggerAdapter(obj) }
        }
    }

    fun whenLimits(limitJSON: JSONObject): List<LimitAdapter> {
        val frequencyLimits = limitJSON.optJSONArray(Constants.INAPP_FC_LIMITS).orEmptyArray()
        val occurrenceLimits = limitJSON.optJSONArray(Constants.INAPP_OCCURRENCE_LIMITS).orEmptyArray()
        return (frequencyLimits.toList<JSONObject>() + occurrenceLimits.toList()).mapNotNull {
            if (it.isNotNullAndEmpty()) LimitAdapter(it) else null
        }
    }
}
