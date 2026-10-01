package com.clevertap.android.sdk.network

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateInAppData
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants
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
 * Wire keys (`eventName`/`responseKey`/`tgtId`) match iOS (SDK-6093) exactly; both platforms treat
 * them as optional/forward-compatible, so gating stays conservative (a missing field still opens the
 * window) rather than assuming presence.
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
     * so the whole fast path stays dormant. When present, the payload carries only the rules that
     * decide a winner — `ti` (from [targetId]), and each of [SELECTION_RULE_KEYS] actually present
     * (forward-compatible). Mirrors iOS (SDK-6093). Display-time keys (mdc/tdc/tlc/efc/rfp) are
     * deliberately excluded — they do not discriminate between candidates.
     */
    fun syntheticInAppPayload(): JSONObject? {
        if (targetId == null || raw.opt(Constants.INAPP_PRIORITY) == null) return null
        return JSONObject().apply {
            put(Constants.INAPP_ID_IN_PAYLOAD, targetId) // ti <- tgtId
            put(Constants.INAPP_SYNTHETIC_CANDIDATE, true)
            for (key in SELECTION_RULE_KEYS) {
                raw.opt(key)?.let { put(key, it) }
            }
        }
    }

    companion object {

        // The selection rules a synthetic candidate copies from the item (if present). These are the
        // only keys that decide a winner during arbitration; display-time caps are excluded. Kept in
        // lockstep with iOS (SDK-6093).
        private val SELECTION_RULE_KEYS = listOf(
            Constants.INAPP_PRIORITY,
            Constants.INAPP_SUPPRESSED,
            InAppDelayConstants.INAPP_DELAY_AFTER_TRIGGER,
            Constants.INAPP_WHEN_TRIGGERS,
            Constants.INAPP_FC_LIMITS,
            Constants.INAPP_OCCURRENCE_LIMITS,
            CustomTemplateInAppData.KEY_TEMPLATE_NAME
        )

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
