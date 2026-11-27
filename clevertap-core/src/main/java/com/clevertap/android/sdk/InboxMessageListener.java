package com.clevertap.android.sdk;

import com.clevertap.android.sdk.inbox.CTInboxMessage;

/**
 * Receives App Inbox item and CTA clicks. Register it with
 * {@link CleverTapAPI#setCTInboxMessageListener(InboxMessageListener)}.
 * <p>
 * Kept in Java on purpose (SDK-6395): Kotlin sees the parameters as platform types, so wrapper SDKs
 * and apps can override them with nullable or non-null types. Do not convert this file to Kotlin.
 *
 * @see InboxMessageButtonListener
 */
public interface InboxMessageListener {
    /**
     * This callback notifies about the following:
     * <ul>
     *   <li>App Inbox item click</li>
     *   <li>CTA clicks for which no custom key-value pairs are associated, i.e., DeepLink and
     *   Clipboard CTAs.</li>
     * </ul>
     *
     * @param message          the instance of {@link CTInboxMessage}
     * @param contentPageIndex the page index of the content. It makes sense for the carousel templates.
     *                         For non-carousel templates, it is 0 as they have only one page of content.
     * @param buttonIndex      the button index corresponds to the CTA button clicked (0, 1, or 2) in
     *                         the App Inbox, which supports up to three CTAs.
     *                         A value of -1 indicates an app inbox item click.
     *                         <p>
     *                         Called on the main thread.
     */
    void onInboxItemClicked(CTInboxMessage message, int contentPageIndex, int buttonIndex);
}
