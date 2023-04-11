package com.clevertap.android.sdk;

import com.clevertap.android.sdk.inbox.CTInboxMessage;

public interface InboxMessageListener {
    /**
     * This callback notifies about the following:
     * - App Inbox item click
     * - CTA clicks for which no custom key-value pairs are associated, i.e., DeepLink and Clipboard CTAs.
     *
     * @param message      - the instance of {@link CTInboxMessage}
     * @param contentPageIndex    - the page index of the content. It makes sense for the carousel templates.
     *                              For non-carousel templates, it is 0 as they have only one page of content.
     * @param buttonIndex  - the button index corresponds to the CTA button clicked (0, 1, or 2) in
     *                       the App Inbox, which supports up to three CTAs.
     *                       A value of -1 indicates an app inbox item click.
     */
    void onInboxItemClicked(CTInboxMessage message, int contentPageIndex, int buttonIndex);
}
