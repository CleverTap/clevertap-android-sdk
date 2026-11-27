package com.clevertap.android.sdk.variables.callbacks

/**
 * Callback for the fetch operation of the variables.
 */
public fun interface FetchVariablesCallback {
    /**
     * Called when the fetch started by `CleverTapAPI.fetchVariables` has finished. Runs on a CleverTap
     * background thread, not the main thread. It is not called when the instance is analytics-only or
     * when the response contains no variables, and only one callback is kept: registering another one
     * replaces it.
     *
     * @param isSuccess `true` if the variables were fetched, `false` if the request failed
     */
    public fun onVariablesFetched(isSuccess: Boolean)
}
