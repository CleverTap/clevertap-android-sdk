package com.clevertap.android.sdk.task;

import java.util.concurrent.Executor;

class FailureExecutable<TResult> extends Executable<TResult> {

    private final OnFailureListener<TResult> mFailureListener;

    public FailureExecutable(final Executor executor, final OnFailureListener<TResult> listener) {
        super(executor);
        mFailureListener = listener;
    }

    @Override
    void execute(final TResult input) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                mFailureListener.onFailure(input);
            }
        });
    }
}