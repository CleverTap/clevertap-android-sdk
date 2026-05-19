package com.clevertap.android.sdk.response

import androidx.annotation.RestrictTo

/**
 * Discriminates how a V2 inbox payload reached [InboxV2Response]:
 *
 * - [FETCH] — direct result of `POST /inbox/v2/getMessages` via `InboxV2Fetcher`.
 *   This is an authoritative, complete snapshot of the user's inbox; the
 *   cross-device delete sweep runs only on this path.
 * - [A1] — live-behaviour campaign riding the `/a1` decorator chain.
 *   Only the messages in the response body are present; "absent from response"
 *   cannot be used as a delete signal here.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal enum class InboxV2DeliverySource { FETCH, A1 }
