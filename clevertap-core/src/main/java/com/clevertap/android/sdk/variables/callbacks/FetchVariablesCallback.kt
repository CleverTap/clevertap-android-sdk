package com.clevertap.android.sdk.variables.callbacks

/**
 * Callback for the fetch operation of the variables.
 */
public fun interface FetchVariablesCallback {
    /**
     * Called when the fetch started by `CleverTapAPI.fetchVariables` has finished. Runs on a CleverTap
     * background thread, not the main thread.
     *
     * @param isSuccess `true` if the variables were fetched, `false` if the request failed
     */
    public fun onVariablesFetched(isSuccess: Boolean)
}
