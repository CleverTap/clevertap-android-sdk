package com.clevertap.android.sdk;

import androidx.annotation.NonNull;

import java.util.HashMap;

/**
 * Receives the custom key-value pairs of an App Inbox button. Register it with
 * {@link CleverTapAPI#setInboxMessageButtonListener(InboxMessageButtonListener)}.
 * <p>
 * Kept in Java on purpose (SDK-6395) so the 8.x bytecode shape stays the same. The parameter
 * carries androidx nullability annotations that match what the SDK passes (SDK-6396), so a Kotlin
 * override must declare the same nullability. Do not convert this file to Kotlin.
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
    void onInboxButtonClick(@NonNull HashMap<String, String> payload);
}
