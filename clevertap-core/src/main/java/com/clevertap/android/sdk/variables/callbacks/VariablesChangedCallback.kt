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

    public abstract fun variablesChanged()
}
