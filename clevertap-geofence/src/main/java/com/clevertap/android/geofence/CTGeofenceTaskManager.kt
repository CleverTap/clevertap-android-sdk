package com.clevertap.android.geofence

import com.clevertap.android.geofence.CTGeofenceAPI.Companion.GEOFENCE_LOG_TAG
import com.clevertap.android.geofence.interfaces.CTGeofenceTask
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import androidx.annotation.RestrictTo

/**
 * Provides methods to post tasks/Runnable to a single threaded queue for processing tasks
 * This class is singleton, so only one queue will be created and shared.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CTGeofenceTaskManager private constructor() {

    private var EXECUTOR_THREAD_ID: Long = 0

    private var es: ExecutorService = Executors.newFixedThreadPool(1)

    /**
     * Use this to safely post a runnable to the async handler.
     * It adds try/catch blocks around the runnable and the handler itself.
     *
     * @param name     unique name to identify task
     * @param runnable runnable to submit to queue
     * @return a Future representing pending completion of the runnable, can be null in case of nested calls
     */
    fun postAsyncSafely(name: String, runnable: Runnable): Future<*>? {
        var future: Future<*>? = null
        try {
            val executeSync = Thread.currentThread().id == EXECUTOR_THREAD_ID

            if (executeSync) {
                // if new task comes from executor thread itself then run it immediately
                // no need to put it in queue
                runnable.run()
            } else {
                future = es.submit(Runnable {
                    EXECUTOR_THREAD_ID = Thread.currentThread().id
                    try {
                        runnable.run()
                    } catch (t: Throwable) {
                        CTGeofenceAPI.getLogger().verbose(
                            GEOFENCE_LOG_TAG,
                            "Executor service: Failed to complete the scheduled task: $name", t
                        )
                    }
                })
            }
        } catch (t: Throwable) {
            CTGeofenceAPI.getLogger()
                .verbose(GEOFENCE_LOG_TAG, "Failed to submit task: $name to the executor service", t)
        }
        return future
    }

    /**
     * Use this to safely post a CTGeofenceTask to the async handler.
     * It adds try/catch blocks around the runnable and the handler itself.
     *
     * @param name unique name to identify task
     * @param task Task to submit to queue
     * @return a Future representing pending completion of the task, can be null in case of nested calls
     */
    fun postAsyncSafely(name: String, task: CTGeofenceTask): Future<*>? {
        var future: Future<*>? = null
        try {
            val executeSync = Thread.currentThread().id == EXECUTOR_THREAD_ID

            if (executeSync) {
                // if new task comes from executor thread itself then run it immediately
                // no need to put it in queue
                task.execute()
            } else {
                future = es.submit(Runnable {
                    EXECUTOR_THREAD_ID = Thread.currentThread().id
                    try {
                        task.execute()
                    } catch (t: Throwable) {
                        CTGeofenceAPI.getLogger().verbose(
                            GEOFENCE_LOG_TAG,
                            "Executor service: Failed to complete the scheduled task: $name", t
                        )
                    }
                })
            }
        } catch (t: Throwable) {
            CTGeofenceAPI.getLogger()
                .verbose(GEOFENCE_LOG_TAG, "Failed to submit task: $name to the executor service", t)
        }

        return future
    }

    fun setExecutorService(es: ExecutorService) {
        this.es = es
    }

    companion object {
        private var taskManager: CTGeofenceTaskManager? = null

        @JvmStatic
        fun getInstance(): CTGeofenceTaskManager {
            synchronized(CTGeofenceTaskManager::class.java) {
                if (taskManager == null) {
                    taskManager = CTGeofenceTaskManager()
                }
                return taskManager!!
            }
        }
    }
}
