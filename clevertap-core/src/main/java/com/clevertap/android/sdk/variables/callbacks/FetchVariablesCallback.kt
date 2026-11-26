package com.clevertap.android.sdk.variables.callbacks

/**
 * Callback for the fetch operation of the variables.
 */
public fun interface FetchVariablesCallback {
    public fun onVariablesFetched(isSuccess: Boolean)
}
