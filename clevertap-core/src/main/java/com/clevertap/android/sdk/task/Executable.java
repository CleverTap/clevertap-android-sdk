package com.clevertap.android.sdk.task;

import java.util.concurrent.Executor;

abstract class Executable<TResult> {
    protected final Executor executor;

    Executable(final Executor executor) {
        this.executor = executor;
    }

    abstract void execute(final TResult input);
}