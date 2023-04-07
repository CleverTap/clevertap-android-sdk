package com.clevertap.android.sdk.variables.callbacks;

/**
 * Variables changed callback.
 *
 * @author Ansh Sachdeva
 */
public abstract class VariablesChangedCallback implements Runnable {
    public void run() {
        this.variablesChanged();
    }

    public abstract void variablesChanged();
}
