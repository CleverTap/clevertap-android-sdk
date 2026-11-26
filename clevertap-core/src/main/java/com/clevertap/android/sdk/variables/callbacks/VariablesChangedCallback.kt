package com.clevertap.android.sdk.variables.callbacks

/**
 * Variables changed callback.
 *
 * @author Ansh Sachdeva
 */
abstract class VariablesChangedCallback : Runnable {
    override fun run() {
        variablesChanged()
    }

    abstract fun variablesChanged()
}
