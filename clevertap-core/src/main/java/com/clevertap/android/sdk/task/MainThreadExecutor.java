package com.clevertap.android.sdk.task;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import java.util.concurrent.Executor;

/**
 * Executor service to delegate runnables to Main Thread.
 */
public class MainThreadExecutor implements Executor {

    void setMainThreadHandler(final Handler mainThreadHandler) {
        this.mainThreadHandler = mainThreadHandler;
    }

    Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    @Override
    public void execute(@NonNull Runnable command) {
        mainThreadHandler.post(command);
    }
}