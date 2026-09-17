package com.clevertap.android.sdk.network

import com.clevertap.android.sdk.Constants
import org.json.JSONArray
import org.json.JSONObject

/**
 * Typed view over a single `content_fetch` directive item carried on an `/a1` response.
 *
 * The item is self-describing: it names the event the fetch is for ([eventName]), the channel key
 * the `/content` reply will arrive under ([responseKey]), and the campaign it hydrates ([targetId]).
 * These are what per-channel arbitration gating keys off (SDK-6141). Today the SDK forwards the raw
 * item straight through as `evtData`; this type only makes the fields addressable and changes no
 * behaviour.
 *
 * TODO(SDK-6141 review): confirm `eventName`/`responseKey` are actually present on the wire for
 * Android (verified on iOS; Android tests use synthetic fixtures). If they are absent, gating must
 * fall back to "gate all in-app content fetches".
 */
internal data class ContentFetchItem(
    val eventName: String?,
    val responseKey: String?,
    val targetId: String?,
    val raw: JSONObject
) {

    /**
     * Builds a synthetic in-app payload from this item's *selection rules* for Option-2 dry-run
     * prediction (SDK-6143 / SDK-6144) — enough to run eligibility + priority sort, never the
     * content itself.
     *
     * Returns null unless the backend has attached `priority` to the item, which it does not today,
     * so the whole fast path stays dormant. When present, the payload carries only the keys the
     * arbitration reads: `ti` (from [targetId]), `priority`, `whenTriggers`, `frequencyLimits`,
     * `occurrenceLimits`. Display-time keys (mdc/tdc/tlc/efc/rfp) are deliberately excluded — they
     * do not discriminate between candidates.
     *
     * TODO(SDK-6144 review): add `templateName` / `suppressed` / `delayAfterTrigger` here if the
     * backend includes them in the item.
     */
    fun syntheticInAppPayload(): JSONObject? {
        val priority = raw.opt(Constants.INAPP_PRIORITY) ?: return null
        return JSONObject().apply {
            targetId?.let { put(Constants.INAPP_ID_IN_PAYLOAD, it) }
            put(Constants.INAPP_PRIORITY, priority)
            raw.opt(Constants.INAPP_WHEN_TRIGGERS)?.let { put(Constants.INAPP_WHEN_TRIGGERS, it) }
            raw.opt(Constants.INAPP_FC_LIMITS)?.let { put(Constants.INAPP_FC_LIMITS, it) }
            raw.opt(Constants.INAPP_OCCURRENCE_LIMITS)?.let { put(Constants.INAPP_OCCURRENCE_LIMITS, it) }
            put(Constants.INAPP_SYNTHETIC_CANDIDATE, true)
        }
    }

    companion object {

        fun from(item: JSONObject): ContentFetchItem = ContentFetchItem(
            eventName = item.optNullableString(Constants.CONTENT_FETCH_ITEM_EVENT_NAME),
            responseKey = item.optNullableString(Constants.CONTENT_FETCH_ITEM_RESPONSE_KEY),
            // tgtId is numeric on the wire while ti may be numeric or string — normalize to String
            // so the two campaign identities can be compared directly during arbitration.
            targetId = item.opt(Constants.CONTENT_FETCH_ITEM_TARGET_ID)?.toString(),
            raw = item
        )

        fun listFrom(items: JSONArray): List<ContentFetchItem> =
            (0 until items.length()).mapNotNull { index ->
                items.optJSONObject(index)?.let { from(it) }
            }

        private fun JSONObject.optNullableString(key: String): String? =
            if (has(key) && !isNull(key)) optString(key).ifEmpty { null } else null
    }
}
