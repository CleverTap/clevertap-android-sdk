package com.clevertap.android.sdk.network

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateInAppData
import com.clevertap.android.sdk.inapp.data.InAppDelayConstants
import org.json.JSONArray
import org.json.JSONObject

/**
 * Typed view over a single `content_fetch` directive item on an `/a1` response: the event it is for
 * ([eventName]), the channel key its `/content` reply arrives under ([responseKey]), and the campaign
 * it hydrates ([targetId]) — what arbitration gating keys off. All optional, so gating stays
 * conservative when a field is absent.
 */
internal data class ContentFetchItem(
    val eventName: String?,
    val responseKey: String?,
    val targetId: String?,
    val raw: JSONObject
) {

    /**
     * Selection-rule-only payload for Option-2 dry-run prediction (never the content). Null without
     * `priority` (so the fast path stays dormant); otherwise `ti` (from [targetId]) plus each present
     * key in [SELECTION_RULE_KEYS]. No display-time keys — they don't discriminate candidates.
     */
    fun syntheticInAppPayload(): JSONObject? {
        if (targetId == null) {
            return null
        }
        // Treat an explicit JSON null or a non-numeric priority as missing — a bad value would
        // otherwise build a synthetic that sortByPriority reads back as the default priority (1).
        raw.opt(Constants.INAPP_PRIORITY)
            ?.takeUnless { it === JSONObject.NULL }
            ?.takeIf { it is Number || (it is String && it.toIntOrNull() != null) }
            ?: return null
        return JSONObject().apply {
            put(Constants.INAPP_ID_IN_PAYLOAD, targetId) // ti <- tgtId
            for (key in SELECTION_RULE_KEYS) {
                raw.opt(key)?.takeUnless { it === JSONObject.NULL }?.let { put(key, it) }
            }
        }
    }

    companion object {

        // Keys that decide a winner during arbitration (copied if present).
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
            // tgtId is numeric while ti may be numeric or string — normalize to String for comparison.
            // An explicit JSON null stays absent rather than becoming the string "null".
            targetId = item.opt(Constants.CONTENT_FETCH_ITEM_TARGET_ID)
                ?.takeUnless { it === JSONObject.NULL }
                ?.toString(),
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
