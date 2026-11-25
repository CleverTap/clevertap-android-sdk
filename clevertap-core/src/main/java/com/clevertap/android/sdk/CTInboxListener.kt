package com.clevertap.android.sdk

import com.clevertap.android.sdk.inbox.CTInboxMessage

interface CTInboxListener {

    /**
     * Receives a callback when inbox controller is initialized
     */
    fun inboxDidInitialize()

    /**
     * Receives a callback when inbox controller updates/deletes/marks as read any [CTInboxMessage] object
     */
    fun inboxMessagesDidUpdate()
}
