package com.clevertap.android.directcall.events;

import androidx.annotation.Nullable;

import com.clevertap.android.directcall.Constants;
import com.clevertap.android.directcall.init.DirectCallAPI;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Provides methods to post tasks/Runnable to a single threaded queue for processing tasks
 * This class is singleton, so only one queue will be created and shared.
 */
public class CTDirectCallTaskManager {

    private static CTDirectCallTaskManager taskManager;

    private long EXECUTOR_THREAD_ID = 0;

    private ExecutorService es;

    private CTDirectCallTaskManager() {
        es = Executors.newFixedThreadPool(1);
    }

    public static synchronized CTDirectCallTaskManager getInstance() {
        if (taskManager == null) {
            taskManager = new CTDirectCallTaskManager();
        }
        return taskManager;
    }

    /**
     * Use this to safely post a CTGeofenceTask to the async handler.
     * It adds try/catch blocks around the runnable and the handler itself.
     *
     * @param name unique name to identify task
     * @param task Task to submit to queue
     * @return a Future representing pending completion of the task, can be null in case of nested calls
     */
    @SuppressWarnings("UnusedParameters")
    @Nullable
    public Future<?> postAsyncSafely(final String name, final CTDirectCallTask task) {
        Future<?> future = null;
        try {
            final boolean executeSync = Thread.currentThread().getId() == EXECUTOR_THREAD_ID;

            if (executeSync) {
                // if new task comes from executor thread itself then run it immediately
                // no need to put it in queue
                task.execute();
            } else {
                future = es.submit(new Runnable() {
                    @Override
                    public void run() {
                        EXECUTOR_THREAD_ID = Thread.currentThread().getId();
                        try {
                            task.execute();
                        } catch (Throwable t) {
                            DirectCallAPI.getLogger().verbose(Constants.CALLING_LOG_TAG_SUFFIX,
                                    "Executor service: Failed to complete the scheduled task: " + name, t);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            DirectCallAPI.getLogger()
                    .verbose(Constants.CALLING_LOG_TAG_SUFFIX, "Failed to submit task: " + name + " to the executor service", t);
        }

        return future;
    }

}
