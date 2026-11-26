package com.clevertap.android.sdk

/**
 * Callback for [CleverTapAPI.fetchInbox].
 *
 * **Thread note:** invoked on the SDK's network dispatcher thread, not
 * the main thread. Post to the main thread yourself if the callback touches
 * UI.
 */
public fun interface FetchInboxCallback {

    /**
     * @param success `true` if messages were fetched and applied to
     *                the local cache; `false` if the fetch was
     *                throttled, disabled for this session, or failed due to
     *                a network/server error.
     */
    public fun onInboxFetched(success: Boolean)
}
