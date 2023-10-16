package com.clevertap.android.sdk.inapp.callbacks

/**
 * Callback for the fetch operation of the InApps.
 */
interface FetchInAppsCallback {
    fun onInAppsFetched(isSuccess: Boolean)
}