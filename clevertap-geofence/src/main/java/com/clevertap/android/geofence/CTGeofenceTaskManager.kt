package com.clevertap.android.geofence;

import static com.clevertap.android.geofence.CTGeofenceAPI.GEOFENCE_LOG_TAG;

import androidx.annotation.Nullable;
import com.clevertap.android.geofence.interfaces.CTGeofenceTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Provides methods to post tasks/Runnable to a single threaded queue for processing tasks
 * This class is singleton, so only one queue will be created and shared.
 */
class CTGeofenceTaskManager {

    private static CTGeofenceTaskManager taskManager;

    private long EXECUTOR_THREAD_ID = 0;

    private ExecutorService es;

    private CTGeofenceTaskManager() {
        es = Executors.newFixedThreadPool(1);
    }

    /**
     * Use this to safely post a runnable to the async handler.
     * It adds try/catch blocks around the runnable and the handler itself.
     *
     * @param name     unique name to identify task
     * @param runnable runnable to submit to queue
     * @return a Future representing pending completion of the runnable, can be null in case of nested calls
     */
    @SuppressWarnings("UnusedParameters")
    @Nullable
    Future<?> postAsyncSafely(final String name, final Runnable runnable) {
        Future<?> future = null;
        try {
            final boolean executeSync = Thread.currentThread().getId() == EXECUTOR_THREAD_ID;

            if (executeSync) {
                // if new task comes from executor thread itself then run it immediately
                // no need to put it in queue
                runnable.run();
            } else {
                future = es.submit(new Runnable() {
                    @Override
                    public void run() {
                        EXECUTOR_THREAD_ID = Thread.currentThread().getId();
                        try {
                            runnable.run();
                        } catch (Throwable t) {
                            CTGeofenceAPI.getLogger().verbose(GEOFENCE_LOG_TAG,
                                    "Executor service: Failed to complete the scheduled task: " + name, t);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            CTGeofenceAPI.getLogger()
                    .verbose(GEOFENCE_LOG_TAG, "Failed to submit task: " + name + " to the executor service", t);
        }
        return future;
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
    Future<?> postAsyncSafely(final String name, final CTGeofenceTask task) {
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
                            CTGeofenceAPI.getLogger().verbose(GEOFENCE_LOG_TAG,
                                    "Executor service: Failed to complete the scheduled task: " + name, t);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            CTGeofenceAPI.getLogger()
                    .verbose(GEOFENCE_LOG_TAG, "Failed to submit task: " + name + " to the executor service", t);
        }

        return future;
    }

    void setExecutorService(ExecutorService es) {
        this.es = es;
    }

    static synchronized CTGeofenceTaskManager getInstance() {
        if (taskManager == null) {
            taskManager = new CTGeofenceTaskManager();
        }
        return taskManager;
    }
}
