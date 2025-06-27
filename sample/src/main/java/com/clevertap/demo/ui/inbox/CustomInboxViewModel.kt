package com.clevertap.demo.ui.inbox

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clevertap.android.sdk.CTInboxListener
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.inbox.CTInboxMessage
import com.clevertap.demo.MyApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CustomInboxViewModel(
    private val cleverTapAPI: CleverTapAPI? = MyApplication.ctInstance
) : ViewModel(), CustomInboxViewModelContract, CTInboxListener {
    override var isInboxInitialized by mutableStateOf(false)
        private set
    override var totalMessagesCount by mutableStateOf(0)
        private set
    override var unreadMessagesCount by mutableStateOf(0)
        private set
    
    private val _inboxMessages = mutableStateListOf<CTInboxMessage>()
    override val inboxMessages: List<CTInboxMessage> = _inboxMessages

    init {
        cleverTapAPI?.ctNotificationInboxListener = this
        cleverTapAPI?.initializeInbox()
    }

    override fun messageToString(message: CTInboxMessage?): String {
        return message.toString()
    }

    override fun inboxDidInitialize() {
        viewModelScope.launch(Dispatchers.Main) {
            isInboxInitialized = true
            updateMessages()
        }
    }

    override fun inboxMessagesDidUpdate() {
        viewModelScope.launch(Dispatchers.Main) {
            updateMessages()
        }
    }

    override fun click(message: CTInboxMessage) {
        cleverTapAPI?.pushInboxNotificationClickedEvent(message.messageId)
    }

    override fun view(message: CTInboxMessage) {
        cleverTapAPI?.pushInboxNotificationViewedEvent(message.messageId)
    }

    override fun markRead(message: CTInboxMessage) {
        cleverTapAPI?.markReadInboxMessage(message)
    }

    override fun delete(message: CTInboxMessage) {
        cleverTapAPI?.deleteInboxMessage(message)
    }

    override fun onCleared() {
        super.onCleared()
        cleverTapAPI?.ctNotificationInboxListener = null
    }

    private fun updateMessages() {
        totalMessagesCount = cleverTapAPI?.inboxMessageCount ?: 0
        unreadMessagesCount = cleverTapAPI?.unreadInboxMessages?.size ?: 0
        _inboxMessages.clear()
        cleverTapAPI?.allInboxMessages?.let { messages ->
            _inboxMessages.addAll(messages)
        }
    }
}
