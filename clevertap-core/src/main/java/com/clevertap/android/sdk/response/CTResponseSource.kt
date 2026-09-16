package com.clevertap.android.sdk.response

/**
 * Identifies which endpoint a response currently flowing through the decorator chain came from.
 *
 * - [A1] — the regular event-batch response from `/a1`. This is the default and covers every
 *   existing flow.
 * - [CONTENT_FETCH] — the response to a `/content` round-trip, re-fed through the *same* decorator
 *   chain by [com.clevertap.android.sdk.network.ContentFetchManager]. Decorators read this to avoid
 *   acting twice on a single logical launch — e.g. the recursion guard (do not fetch again) and the
 *   client-side in-app store guard (do not overwrite a persisted store from a partial response).
 *
 * Threaded per response by [ClevertapResponseHandler], mirroring the existing
 * [CleverTapResponse.isFullResponse] field.
 */
internal enum class CTResponseSource {
    A1,
    CONTENT_FETCH
}
