package com.clevertap.android.sdk;

import com.clevertap.android.sdk.inbox.CTInboxMessage;

public interface CTInboxListener {

    /**
     * Receives a callback when inbox controller is initialized
     */
    void inboxDidInitialize();

    /**
     * Receives a callback when inbox controller updates/deletes/marks as read any {@link CTInboxMessage} object
     */
    void inboxMessagesDidUpdate();
}
