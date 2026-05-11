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
}
