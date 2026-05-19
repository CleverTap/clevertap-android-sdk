package com.clevertap.android.sdk.db.dao

import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.inbox.CTMessageDAO

interface InboxMessageDAO {
    @WorkerThread
    fun getMessages(userId: String): ArrayList<CTMessageDAO>
    
    @WorkerThread
    fun upsertMessages(inboxMessages: List<CTMessageDAO>)
    
    @WorkerThread
    fun deleteMessage(messageId: String, userId: String): Boolean
    
    @WorkerThread
    fun deleteMessages(messageIds: List<String>, userId: String): Boolean
    
    @WorkerThread
    fun markMessageAsRead(messageId: String, userId: String): Boolean
    
    @WorkerThread
    fun markMessagesAsRead(messageIds: List<String>, userId: String): Boolean

    /**
     * Bulk-flips the supplied V2 inbox rows to
     * [com.clevertap.android.sdk.inbox.InboxIndexState.INDEXED]. Called
     * from the FETCH path so any [com.clevertap.android.sdk.inbox.InboxIndexState.PENDING_INDEXING]
     * row that just appeared in fetch is promoted (the upsert path
     * preserves [com.clevertap.android.sdk.db.Column.INDEX_STATE] on
     * UPDATE; this UPDATE is the authoritative state transition).
     */
    @WorkerThread
    fun markIndexed(messageIds: List<String>, userId: String): Boolean

    /**
     * Returns the `_id`s of V2 inbox rows that are candidates for the
     * cross-device delete sweep run after each fetch response:
     *  - Rows with `index_state = 'INDEXED'` — fetch backend has confirmed
     *    these before; absence from the current response is a reliable
     *    delete signal.
     *  - Rows with `index_state = 'PENDING_INDEXING'` whose `created_at`
     *    is older than [staleCutoffSeconds] — the indexing grace window has
     *    demonstrably elapsed; absence is treated as a delete signal too.
     *
     * V1 messages are never included (`source = 'V1'` rows are excluded).
     *
     * @param userId             User whose rows to inspect.
     * @param staleCutoffSeconds Epoch-seconds cutoff; `PENDING_INDEXING` rows
     *   with `created_at < staleCutoffSeconds` are included.
     * @return Set of `_id` values; never null, may be empty.
     */
    @WorkerThread
    fun findSweepableV2Ids(userId: String, staleCutoffSeconds: Long): Set<String>
}
