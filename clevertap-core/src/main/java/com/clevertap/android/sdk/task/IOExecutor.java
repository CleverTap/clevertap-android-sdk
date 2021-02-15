package com.clevertap.android.sdk.task;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

class IOExecutor implements Executor {

    private final int numCores = Runtime.getRuntime().availableProcessors();

    private final Executor mExecutor = new ThreadPoolExecutor(numCores * 2, numCores * 2,
            60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

    @Override
    public void execute(final Runnable command) {
        mExecutor.execute(command);
    }
}
