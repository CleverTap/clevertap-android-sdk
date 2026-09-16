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
