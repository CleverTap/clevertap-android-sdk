package com.clevertap.android.sdk.variables.callbacks

/**
 * Callback for the fetch operation of the variables.
 */
fun interface FetchVariablesCallback {
    fun onVariablesFetched(isSuccess: Boolean)
}
