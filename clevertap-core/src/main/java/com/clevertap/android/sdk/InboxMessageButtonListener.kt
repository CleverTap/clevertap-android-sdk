package com.clevertap.android.sdk

import java.util.HashMap

/**
 * Receives the custom key-value pairs of an App Inbox button. Register it with
 * [CleverTapAPI.setInboxMessageButtonListener].
 *
 * @see InboxMessageListener
 */
public fun interface InboxMessageButtonListener {

    /**
     * callback to transfer payload when inbox button is clicked
     *
     * Called on the main thread, only for buttons that carry key-value pairs, and after
     * [InboxMessageListener.onInboxItemClicked].
     *
     * @param payload the key-value pairs configured on the clicked button
     */
    public fun onInboxButtonClick(payload: HashMap<String, String>?)
}
