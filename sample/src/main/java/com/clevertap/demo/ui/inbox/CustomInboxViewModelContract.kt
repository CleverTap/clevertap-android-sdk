package com.clevertap.demo.ui.inbox

import com.clevertap.android.sdk.inbox.CTInboxMessage

interface CustomInboxViewModelContract {
    val isInboxInitialized: Boolean
    val totalMessagesCount: Int
    val unreadMessagesCount: Int
    val inboxMessages: List<CTInboxMessage>

    fun messageToString(message: CTInboxMessage?): String
    fun click(message: CTInboxMessage)
    fun view(message: CTInboxMessage)
    fun markRead(message: CTInboxMessage)
    fun delete(message: CTInboxMessage)
}
