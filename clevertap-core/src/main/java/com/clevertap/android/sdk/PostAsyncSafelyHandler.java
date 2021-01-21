package com.clevertap.android.sdk;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.RestrictTo.Scope;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RestrictTo(Scope.LIBRARY_GROUP)
public class PostAsyncSafelyHandler {

    private long EXECUTOR_THREAD_ID = 0;

    private long NOTIFICATION_THREAD_ID = 0;

    private final ExecutorService ns;

    private final ExecutorService es;

    private final CleverTapInstanceConfig mConfig;

    PostAsyncSafelyHandler(CoreState coreState) {
        mConfig = coreState.getConfig();
        this.es = Executors.newFixedThreadPool(1);
        this.ns = Executors.newFixedThreadPool(1);
    }

    /**
     * Use this to safely post a runnable to the async handler.
     * It adds try/catch blocks around the runnable and the handler itself.
     */
    @SuppressWarnings("UnusedParameters")
    @Nullable
    public Future<?> postAsyncSafely(final String name, final Runnable runnable) {
        Future<?> future = null;
        try {
            final boolean executeSync = Thread.currentThread().getId() == EXECUTOR_THREAD_ID;

            if (executeSync) {
                runnable.run();
            } else {
                future = es.submit(new Runnable() {
                    @Override
                    public void run() {
                        EXECUTOR_THREAD_ID = Thread.currentThread().getId();
                        try {
                            runnable.run();
                        } catch (Throwable t) {
                            mConfig.getLogger().verbose(mConfig.getAccountId(),
                                    "Executor service: Failed to complete the scheduled task", t);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            mConfig.getLogger().verbose(mConfig.getAccountId(), "Failed to submit task to the executor service", t);
        }

        return future;
    }

    //InApp
    void runOnNotificationQueue(final Runnable runnable) {
        try {
            final boolean executeSync = Thread.currentThread().getId() == NOTIFICATION_THREAD_ID;

            if (executeSync) {
                runnable.run();
            } else {
                ns.submit(new Runnable() {
                    @Override
                    public void run() {
                        NOTIFICATION_THREAD_ID = Thread.currentThread().getId();
                        try {
                            runnable.run();
                        } catch (Throwable t) {
                            mConfig.getLogger().verbose(mConfig.getAccountId(),
                                    "Notification executor service: Failed to complete the scheduled task", t);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            mConfig.getLogger()
                    .verbose(mConfig.getAccountId(), "Failed to submit task to the notification executor service", t);
        }
    }
}
