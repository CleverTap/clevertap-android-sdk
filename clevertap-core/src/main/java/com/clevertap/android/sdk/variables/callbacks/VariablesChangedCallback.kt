package com.clevertap.android.sdk.variables.callbacks

/**
 * Variables changed callback.
 *
 * @author Ansh Sachdeva
 */
public abstract class VariablesChangedCallback : Runnable {
    override fun run() {
        variablesChanged()
    }

    /**
     * Called when variable values have been updated from the server, or loaded from the stored values
     * when the fetch failed. Runs on the main thread, except when the callback is registered after the
     * first fetch request has already completed (successfully or not): then it is called immediately on
     * the thread that registered it.
     */
    public abstract fun variablesChanged()
}
