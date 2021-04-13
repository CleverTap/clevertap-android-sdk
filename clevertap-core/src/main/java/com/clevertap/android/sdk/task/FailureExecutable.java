package com.clevertap.android.sdk.task;

import java.util.concurrent.Executor;

/**
 * Wrapper class to execute runnable after a task is failed.
 * Ref{@link OnFailureListener}
 *
 * @param <TResult>
 */
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