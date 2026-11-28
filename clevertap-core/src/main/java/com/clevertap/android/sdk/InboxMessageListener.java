package com.clevertap.android.sdk;

import androidx.annotation.NonNull;

import com.clevertap.android.sdk.inbox.CTInboxMessage;

/**
 * Receives App Inbox item and CTA clicks. Register it with
 * {@link CleverTapAPI#setCTInboxMessageListener(InboxMessageListener)}.
 * <p>
 * Kept in Java on purpose (SDK-6395) so the 8.x bytecode shape stays the same. The parameters carry
 * androidx nullability annotations that match what the SDK passes (SDK-6396), so a Kotlin override
 * must declare the same nullability. Do not convert this file to Kotlin.
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
    void onInboxItemClicked(@NonNull CTInboxMessage message, int contentPageIndex, int buttonIndex);
}
