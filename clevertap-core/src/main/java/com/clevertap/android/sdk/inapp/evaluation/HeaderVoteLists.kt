package com.clevertap.android.sdk.inapp.evaluation

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.isNotNullAndEmpty
import com.clevertap.android.sdk.variables.JsonUtil
import org.json.JSONArray
import org.json.JSONObject

/**
 * Channel-agnostic algorithms for the `/a1` header "vote" lists — the evaluated-campaign ids and the
 * CG/suppressed acks each evaluator attaches, then removes exactly what was sent. Shared by in-app
 * (`inapps_eval`/`inapps_suppressed`) and ND (`adUnit_eval`/`adUnit_suppressed`); only the wire keys
 * and persistence sinks differ. The lists themselves stay owned by each evaluator.
 */
internal object HeaderVoteLists {

    /**
     * Parses persisted eval ids without dropping int-range values (`org.json` deserializes int-range
     * numbers as `Integer`, which a reified `toList<Long>()` would silently discard).
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
        // Match on the exact wzrk_id of each sent entry, not a substring of the serialized array: a
        // wzrk_id like "234_20260915" is a substring of "1234_20260915", so `sentString.contains(id)`
        // would drop an ack that was never sent (losing that CG-suppression report).
        val sentIds = (0 until sent.length())
            .mapNotNull { sent.optJSONObject(it)?.optString(Constants.NOTIFICATION_ID_TAG)?.takeIf(String::isNotEmpty) }
            .toSet()
        var updated = false
        val iterator = suppressed.iterator()
        while (iterator.hasNext()) {
            val id = iterator.next()[Constants.NOTIFICATION_ID_TAG] as? String
            if (id != null && id in sentIds) {
                iterator.remove()
                updated = true
            }
        }
        return updated
    }
}
