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
}
