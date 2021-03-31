package com.clevertap.android.sdk.task;

import java.util.concurrent.Executor;

/**
 * Stuffs which can be executed on an executor with certain input parameter
 * @param <TResult>
 */
abstract class Executable<TResult> {
    protected final Executor executor;

    Executable(final Executor executor) {
        this.executor = executor;
    }

    abstract void execute(final TResult input);
}