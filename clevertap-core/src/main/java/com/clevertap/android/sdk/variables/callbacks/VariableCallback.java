package com.clevertap.android.sdk.variables.callbacks;

import com.clevertap.android.sdk.variables.Var;

/**
 * Callback registered individually to a Var object.
 */
public abstract class VariableCallback<T> implements Runnable {
    private Var<T> variable;

    public void setVariable(Var<T> variable) {
        this.variable = variable;
    }

    public void run() {
        synchronized (variable) {
            this.onValueChanged(variable);
        }
    }

    public abstract void onValueChanged(Var<T> variable);
}
