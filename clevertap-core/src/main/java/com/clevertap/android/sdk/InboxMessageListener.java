package com.clevertap.android.sdk;

import com.clevertap.android.sdk.inbox.CTInboxMessage;

public interface InboxMessageListener {
    /**
     * This callback notifies about the following:
     * - App Inbox item click
     * - CTA clicks for which no custom key-value pairs are associated.
     *
     * @param message     - the instance of {@link CTInboxMessage}
     * @param buttonIndex - the button index corresponds to the CTA button clicked (0, 1, or 2) in
     *                      the App Inbox, which supports up to three CTAs.
     *                      A value of -1 indicates an app inbox item click.
     */
    void onInboxItemClicked(CTInboxMessage message, int buttonIndex);
}
