package com.clevertap.android.sdk

import com.clevertap.android.sdk.inbox.CTInboxMessage

/**
 * Receives App Inbox item and CTA clicks. Register it with [CleverTapAPI.setCTInboxMessageListener].
 *
 * @see InboxMessageButtonListener
 */
public fun interface InboxMessageListener {
    /**
     * This callback notifies about the following:
     * - App Inbox item click
     * - CTA clicks for which no custom key-value pairs are associated, i.e., DeepLink and Clipboard CTAs.
     *
     * Called on the main thread.
     *
     * @param message      - the instance of [CTInboxMessage]
     * @param contentPageIndex    - the page index of the content. It makes sense for the carousel templates.
     *                              For non-carousel templates, it is 0 as they have only one page of content.
     * @param buttonIndex  - the button index corresponds to the CTA button clicked (0, 1, or 2) in
     *                       the App Inbox, which supports up to three CTAs.
     *                       A value of -1 indicates an app inbox item click.
     */
    public fun onInboxItemClicked(message: CTInboxMessage?, contentPageIndex: Int, buttonIndex: Int)
}
