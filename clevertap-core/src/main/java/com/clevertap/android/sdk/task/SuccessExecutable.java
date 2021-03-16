package com.clevertap.android.sdk.task;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import java.util.concurrent.Executor;

class SuccessExecutable<TResult> extends Executable<TResult> {

    private final CleverTapInstanceConfig config;//TODO remove??

    private final OnSuccessListener<TResult> successListener;

    protected SuccessExecutable(final Executor executor, OnSuccessListener<TResult> listener,
            final CleverTapInstanceConfig config) {
        super(executor);
        successListener = listener;
        this.config = config;
    }

    public OnSuccessListener<TResult> getSuccessListener() {
        return successListener;
    }

    @Override
    void execute(final TResult input) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                successListener.onSuccess(input);
            }
        });
    }
}