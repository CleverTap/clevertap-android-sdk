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
     * Called when variable values have been updated from the server. Runs on the main thread, except
     * when the callback is registered after variables were already fetched: then it is called
     * immediately on the thread that registered it.
     */
    public abstract fun variablesChanged()
}
