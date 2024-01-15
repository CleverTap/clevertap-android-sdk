package com.clevertap.android.sdk.inapp.callbacks

/**
 * Callback for the fetch operation of the InApps.
 */
interface FetchInAppsCallback {
    /**
     * Called when the In-App messages are fetched, providing the fetch result.
     *
     * @param isSuccess A boolean value indicating whether the fetch operation was successful.
     *                  When `true`, it indicates a successful fetch. When `false`, there may have
     *                  been an issue with fetching In-App messages or no network or CleverTap
     *                  instance is set to offline mode .
     */
    fun onInAppsFetched(isSuccess: Boolean)
}