package com.clevertap.android.sdk.task;

import java.util.concurrent.Executor;

/**
 * Wrapper class to execute runnable after a task is successful.
 * Ref: {@link OnSuccessListener}
 *
 * @param <TResult>
 */
class SuccessExecutable<TResult> extends Executable<TResult> {

    private final OnSuccessListener<TResult> successListener;

    protected SuccessExecutable(final Executor executor, OnSuccessListener<TResult> listener) {
        super(executor);
        successListener = listener;
    }

    public OnSuccessListener<TResult> getSuccessListener() {
        return successListener;
    }

    @Override
    void execute(final TResult input) {
        executor.execute(() -> successListener.onSuccess(input));
    }
}