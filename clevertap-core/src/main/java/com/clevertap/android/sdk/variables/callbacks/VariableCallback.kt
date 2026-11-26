package com.clevertap.android.sdk.variables.callbacks

import com.clevertap.android.sdk.variables.Var

/**
 * Callback registered individually to a Var object.
 */
public abstract class VariableCallback<T> : Runnable {

    private var variable: Var<T>? = null

    /**
     * Sets the variable passed to [onValueChanged] when this callback runs. The SDK calls it before
     * posting the callback; you don't need to.
     *
     * @param variable the variable
     */
    public open fun setVariable(variable: Var<T>?) {
        this.variable = variable
    }

    override fun run() {
        // the SDK always sets the variable before posting this callback
        val variable = checkNotNull(variable) { "VariableCallback run without a variable" }
        synchronized(variable) {
            onValueChanged(variable)
        }
    }

    /**
     * Called with the variable when its value changed (or, for a file ready handler, when its file is
     * ready). Runs on the main thread, except the immediate call made by [Var.addValueChangedCallback]
     * when variables were already fetched, which runs on the thread that registered the callback.
     *
     * @param variable the variable
     */
    public abstract fun onValueChanged(variable: Var<T>)
}
