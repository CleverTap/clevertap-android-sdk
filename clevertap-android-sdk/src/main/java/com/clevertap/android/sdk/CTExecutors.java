package com.clevertap.android.sdk;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Global executor pools for the whole application.
 * <p>
 * Grouping tasks like this avoids the effects of task starvation (e.g. disk reads don't wait behind
 * webservice requests).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CTExecutors {
    private static CTExecutors sInstance;
    //currently we are not using networkIO using this class, so setting thread count to 0.
    private static final int THREAD_COUNT = 0;

    private final Executor diskIO;

    private final Executor networkIO;

    private final Executor mainThread;


    public static CTExecutors getInstance() {
        if (sInstance == null) {
            synchronized (CTExecutors.class) {
                sInstance = new CTExecutors(Executors.newSingleThreadExecutor(), Executors.newFixedThreadPool(THREAD_COUNT),
                        new MainThreadExecutor());
            }
        }
        return sInstance;
    }

    private CTExecutors(Executor diskIO, Executor networkIO, Executor mainThread) {
        this.diskIO = diskIO;
        this.networkIO = networkIO;
        this.mainThread = mainThread;
    }

    public Executor diskIO() {
        return diskIO;
    }

    public Executor networkIO() {
        return networkIO;
    }

    public Executor mainThread() {
        return mainThread;
    }

    private static class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}