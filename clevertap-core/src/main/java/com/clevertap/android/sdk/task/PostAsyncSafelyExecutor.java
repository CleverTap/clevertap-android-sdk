package com.clevertap.android.sdk.task;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Post async safely executor is nothing but a single thread pool executor
 */
class PostAsyncSafelyExecutor implements Executor {

    private final Executor mExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void execute(final Runnable command) {
        mExecutor.execute(command);
    }
}