package com.clevertap.android.sdk.feat_variable;

public abstract class VariableCallback<T> implements Runnable {
    private Var<T> variable;

    public void setVariable(Var<T> variable) {
        this.variable = variable;
    }

    public void run() {
        synchronized (variable) {
            this.handle(variable);
        }
    }

    public abstract void handle(Var<T> variable);
}
