package com.clevertap.android.sdk

import com.clevertap.android.sdk.inbox.CTInboxMessage

/**
 * Receives App Inbox lifecycle callbacks. Register it with
 * [CleverTapAPI.setCTNotificationInboxListener] before calling [CleverTapAPI.initializeInbox].
 */
public interface CTInboxListener {

    /**
     * Receives a callback when inbox controller is initialized
     *
     * Called on a CleverTap background thread, not the main thread.
     */
    public fun inboxDidInitialize()

    /**
     * Receives a callback when inbox controller updates/deletes/marks as read any [CTInboxMessage] object
     *
     * Called on the main thread.
     */
    public fun inboxMessagesDidUpdate()
}
