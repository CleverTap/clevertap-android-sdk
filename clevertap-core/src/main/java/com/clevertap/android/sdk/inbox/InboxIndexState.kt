package com.clevertap.android.sdk.inbox

import androidx.annotation.RestrictTo

/**
 * String constants for the `inboxMessages.index_state` column.
 *
 * - [PENDING_INDEXING] — message landed locally (typically via `/a1`
 *   live-behaviour push) but the fetch backend may not have indexed
 *   it yet. Kept out of the cross-device delete sweep until either it
 *   appears in a fetch response (flips to [INDEXED]) or the indexing
 *   grace window has demonstrably elapsed.
 * - [INDEXED] — message has appeared in at least one fetch response,
 *   so the fetch backend definitely knows about it. Absence from a
 *   subsequent fetch is treated as a cross-device delete.
 *
 * Lives only on the internal [CTMessageDAO] and the
 * `inboxMessages.index_state` column. Never written into any JSON
 * exposed to the public [CTInboxMessage] model.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object InboxIndexState {
    const val PENDING_INDEXING = "PENDING_INDEXING"
    const val INDEXED = "INDEXED"
}
