package com.clevertap.android.sdk.task;

import com.clevertap.android.sdk.CleverTapInstanceConfig;
import java.util.concurrent.Executor;

class SuccessExecutable<TResult> extends Executable<TResult> {

    private final CleverTapInstanceConfig mConfig;

    private final OnSuccessListener<TResult> mSuccessListener;

    protected SuccessExecutable(final Executor executor, OnSuccessListener<TResult> listener,
            final CleverTapInstanceConfig config) {
        super(executor);
        mSuccessListener = listener;
        mConfig = config;
    }

    public OnSuccessListener<TResult> getSuccessListener() {
        return mSuccessListener;
    }

    @Override
    void execute(final TResult input) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mSuccessListener.onSuccess(input);
            }
        });
    }
}