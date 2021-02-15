package com.clevertap.android.sdk.task;

import java.util.concurrent.Executor;

class SuccessExecutable<TResult> extends Executable<TResult> {

    private final OnSuccessListener<TResult> mSuccessListener;

    protected SuccessExecutable(final Executor executor, OnSuccessListener<TResult> listener) {
        super(executor);
        mSuccessListener = listener;
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