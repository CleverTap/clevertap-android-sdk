package com.clevertap.android.pushtemplates;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class AsyncHelper {

    private long EXECUTOR_THREAD_ID = 0;
    private final ExecutorService es;

    private static AsyncHelper asyncHelperInstance = null;

    private AsyncHelper() {
        this.es = Executors.newFixedThreadPool(1);
    }

    static AsyncHelper getInstance() {
        if (asyncHelperInstance == null) {
            asyncHelperInstance = new AsyncHelper();
        }
        return asyncHelperInstance;
    }

    @SuppressWarnings({"UnusedParameters", "SameParameterValue"})
    void postAsyncSafely(final String name, final Runnable runnable) {
        try {
            final boolean executeSync = Thread.currentThread().getId() == EXECUTOR_THREAD_ID;

            if (executeSync) {
                runnable.run();
            } else {
                this.es.submit(new Runnable() {
                    @Override
                    public void run() {
                        EXECUTOR_THREAD_ID = Thread.currentThread().getId();
                        try {
                            runnable.run();
                        } catch (Throwable t) {
                            PTLog.verbose("Executor service: Failed to complete the scheduled task" + name);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            PTLog.verbose("Failed to submit task to the executor service");
        }
    }

    public static Handler getMainThreadHandler() {
        Handler mainThreadHandler;
        mainThreadHandler = new Handler(Looper.getMainLooper());
        return mainThreadHandler;
    }
}
