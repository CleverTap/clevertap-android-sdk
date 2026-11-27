package com.clevertap.android.sdk.variables.callbacks;

import com.clevertap.android.sdk.variables.Var;

/**
 * Callback registered individually to a Var object.
 * <p>
 * Kept in Java on purpose (SDK-6395): Kotlin sees {@code Var<T>} as a platform type, so wrapper
 * SDKs and apps can override {@link #onValueChanged(Var)} with a nullable or non-null type. Do not
 * convert this file to Kotlin.
 *
 * @param <T> the variable's value type
 */
public abstract class VariableCallback<T> implements Runnable {

    private Var<T> variable;

    /**
     * Sets the variable passed to {@link #onValueChanged(Var)} when this callback runs. The SDK calls
     * it before posting the callback; you don't need to.
     *
     * @param variable the variable
     */
    public void setVariable(Var<T> variable) {
        this.variable = variable;
    }

    @Override
    public void run() {
        // The SDK always sets the variable before posting this callback.
        synchronized (variable) {
            this.onValueChanged(variable);
        }
    }

    /**
     * Called with the variable when its value changed (or, for a file ready handler, when its file is
     * ready). Runs on the main thread, except the immediate call made by
     * {@link Var#addValueChangedCallback(VariableCallback)} when variables were already fetched, which
     * runs on the thread that registered the callback.
     *
     * @param variable the variable
     */
    public abstract void onValueChanged(Var<T> variable);
}
