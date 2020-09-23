package com.clevertap.android.sdk;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

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

    private final Executor diskIO;

    private final Executor mainThread;

    private CTExecutors(Executor diskIO, Executor mainThread) {
        this.diskIO = diskIO;
        this.mainThread = mainThread;
    }

    public static synchronized CTExecutors getInstance() {
        if (sInstance == null) {
            sInstance = new CTExecutors(Executors.newSingleThreadExecutor(),
                    new MainThreadExecutor());
        }
        return sInstance;
    }

    public Executor diskIO() {
        return diskIO;
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