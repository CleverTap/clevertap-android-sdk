package com.clevertap.android.sdk

import com.clevertap.android.sdk.inbox.CTInboxMessage

public interface CTInboxListener {

    /**
     * Receives a callback when inbox controller is initialized
     */
    public fun inboxDidInitialize()

    /**
     * Receives a callback when inbox controller updates/deletes/marks as read any [CTInboxMessage] object
     */
    public fun inboxMessagesDidUpdate()
}
