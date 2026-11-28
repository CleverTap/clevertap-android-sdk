package com.clevertap.android.sdk.variables.callbacks;

import androidx.annotation.NonNull;

import com.clevertap.android.sdk.variables.Var;

/**
 * Callback registered individually to a Var object.
 * <p>
 * Kept in Java on purpose (SDK-6395) so the 8.x bytecode shape stays the same. The parameter of
 * {@link #onValueChanged(Var)} carries androidx nullability annotations that match what the SDK
 * passes (SDK-6396), so a Kotlin override must declare the same nullability. Do not convert this
 * file to Kotlin.
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
    public abstract void onValueChanged(@NonNull Var<T> variable);
}
