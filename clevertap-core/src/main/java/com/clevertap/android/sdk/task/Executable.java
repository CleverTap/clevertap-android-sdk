package com.clevertap.android.sdk.task;

import java.util.concurrent.Executor;

abstract class Executable<TResult> {
    protected final Executor mExecutor;

    Executable(final Executor executor) {
        mExecutor = executor;
    }

    abstract void execute(final TResult input);
}