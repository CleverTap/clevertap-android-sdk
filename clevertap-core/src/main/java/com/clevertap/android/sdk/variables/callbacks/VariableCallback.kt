package com.clevertap.android.sdk.variables.callbacks

import com.clevertap.android.sdk.variables.Var

/**
 * Callback registered individually to a Var object.
 */
abstract class VariableCallback<T> : Runnable {

    private var variable: Var<T>? = null

    open fun setVariable(variable: Var<T>?) {
        this.variable = variable
    }

    override fun run() {
        val variable = variable!!
        synchronized(variable) {
            this.onValueChanged(variable)
        }
    }

    abstract fun onValueChanged(variable: Var<T>)
}
