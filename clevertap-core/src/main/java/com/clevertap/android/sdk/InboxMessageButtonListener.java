package com.clevertap.android.sdk;

import java.util.HashMap;

/**
 * Receives the custom key-value pairs of an App Inbox button. Register it with
 * {@link CleverTapAPI#setInboxMessageButtonListener(InboxMessageButtonListener)}.
 * <p>
 * Kept in Java on purpose (SDK-6395): Kotlin sees the parameter as a platform type, so wrapper SDKs
 * and apps can override it with a nullable or non-null type. Do not convert this file to Kotlin.
 *
 * @see InboxMessageListener
 */
public interface InboxMessageButtonListener {

    /**
     * callback to transfer payload when inbox button is clicked
     * <p>
     * Called on the main thread, only for buttons that carry key-value pairs, and after
     * {@link InboxMessageListener#onInboxItemClicked}.
     *
     * @param payload the key-value pairs configured on the clicked button
     */
    void onInboxButtonClick(HashMap<String, String> payload);
}
