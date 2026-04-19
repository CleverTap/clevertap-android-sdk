package com.clevertap.android.sdk;

/**
 * Callback for {@link CleverTapAPI#fetchInbox(FetchInboxCallback)}.
 *
 * <p><b>Thread note:</b> invoked on the SDK's network dispatcher thread, not
 * the main thread. Post to the main thread yourself if the callback touches
 * UI.
 */
public interface FetchInboxCallback {

    /**
     * @param success {@code true} if messages were fetched and applied to
     *                the local cache; {@code false} if the fetch was
     *                throttled, disabled for this session, or failed due to
     *                a network/server error.
     */
    void onInboxFetched(boolean success);
}
