package com.clevertap.android.sdk.task;

import java.util.concurrent.Executor;

class FailureExecutable<TResult> extends Executable<TResult> {

    public OnFailureListener<TResult> getFailureListener() {
        return failureListener;
    }

    private final OnFailureListener<TResult> failureListener;

    public FailureExecutable(final Executor executor, final OnFailureListener<TResult> listener) {
        super(executor);
        failureListener = listener;
    }

    @Override
    void execute(final TResult input) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                failureListener.onFailure(input);
            }
        });
    }
}