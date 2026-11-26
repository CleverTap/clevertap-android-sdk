package com.clevertap.android.sdk.variables.callbacks

import com.clevertap.android.sdk.variables.Var

/**
 * Callback registered individually to a Var object.
 */
public abstract class VariableCallback<T> : Runnable {

    private var variable: Var<T>? = null

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

    public abstract fun onValueChanged(variable: Var<T>)
}
