package com.clevertap.android.sdk.inapp.evaluation

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.isNotNullAndEmpty
import com.clevertap.android.sdk.variables.JsonUtil
import org.json.JSONArray
import org.json.JSONObject

/**
 * Channel-agnostic logic for the `/a1` header "vote" lists — the evaluated-campaign ids and the
 * CG/suppressed acks that each evaluator attaches and then removes-exactly-what-was-sent.
 *
 * Only the wire keys and the persistence sinks differ per channel (in-app `inapps_eval` /
 * `inapps_suppressed` vs ND `adUnit_eval` / `adUnit_suppressed`); the attach, remove-exactly-sent,
 * and int-safe id parsing are identical. Keeping them here removes the copy-paste that already caused
 * drift — the `optLong` reload fix (F3) previously had to be applied in two places
 * (prototype B — SDK-6055 design review).
 *
 * State (the actual lists) stays owned by each evaluator; this object only holds the shared algorithms.
 */
internal object HeaderVoteLists {

    /**
     * Parses persisted eval ids without dropping int-range values. `org.json` deserializes numeric
     * literals within `Int` range as `Integer`, so a reified `toList<Long>()` filter would silently
     * discard every id (campaign ids are epoch-second, always int-range).
     */
    fun readEvalIds(stored: JSONArray): MutableList<Long> =
        (0 until stored.length()).map { stored.optLong(it) }.filter { it != 0L }.toMutableList()

    /** Builds the header contribution, or null if both lists are empty. */
    fun attach(
        evalMetaKey: String,
        suppressedMetaKey: String,
        evalIds: List<Long>,
        suppressed: List<Map<String, Any?>>
    ): JSONObject? {
        val header = JSONObject()
        if (evalIds.isNotEmpty()) {
            header.put(evalMetaKey, JsonUtil.listToJsonArray(evalIds))
        }
        if (suppressed.isNotEmpty()) {
            header.put(suppressedMetaKey, JsonUtil.listToJsonArray(suppressed))
        }
        return if (header.isNotNullAndEmpty()) header else null
    }

    /**
     * Removes exactly the eval ids present in the sent header (a concurrent vote added after attach
     * but not in the sent header survives). Returns true if the caller should persist.
     */
    fun removeSentEvalIds(sentHeader: JSONObject, evalMetaKey: String, evalIds: MutableList<Long>): Boolean {
        var updated = false
        sentHeader.optJSONArray(evalMetaKey)?.let {
            for (i in 0 until it.length()) {
                val id = it.optLong(i)
                if (id != 0L) {
                    updated = true
                    evalIds.remove(id)
                }
            }
        }
        return updated
    }

    /**
     * Removes suppressed acks whose id appears in the sent header. Returns true if the caller should
     * persist.
     */
    fun removeSentSuppressed(
        sentHeader: JSONObject,
        suppressedMetaKey: String,
        suppressed: MutableList<Map<String, Any?>>
    ): Boolean {
        val sent = sentHeader.optJSONArray(suppressedMetaKey) ?: return false
        val sentString = sent.toString()
        var updated = false
        val iterator = suppressed.iterator()
        while (iterator.hasNext()) {
            val id = iterator.next()[Constants.NOTIFICATION_ID_TAG] as? String
            if (id != null && sentString.contains(id)) {
                iterator.remove()
                updated = true
            }
        }
        return updated
    }
}
