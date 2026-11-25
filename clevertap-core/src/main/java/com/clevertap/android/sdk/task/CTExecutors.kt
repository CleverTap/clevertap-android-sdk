package com.clevertap.android.sdk.task

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.utils.UrlHashGenerator
import java.util.concurrent.Executor

/**
 * Global executor pools per account.
 *
 * Grouping tasks like this avoids the effects of task starvation (e.g. disk reads don't wait behind
 * webservice requests).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class CTExecutors {

    @JvmField
    val IO_EXECUTOR: IOExecutor

    @JvmField
    val MAIN_EXECUTOR: MainThreadExecutor = MainThreadExecutor()

    @JvmField
    val CURRENT_THREAD_EXECUTOR: Executor = Executor { it.run() }

    @JvmField
    val DEFAULT_CALLBACK_EXECUTOR: MainThreadExecutor = MAIN_EXECUTOR

    @JvmField
    protected val config: CleverTapInstanceConfig?

    @JvmField
    protected var singleThreadExecutorTag: String? = null

    private val postAsyncSafelyTasks = HashMap<String, PostAsyncSafelyExecutor>()

    internal constructor(config: CleverTapInstanceConfig?) {
        this.config = config
        IO_EXECUTOR = IOExecutor()
    }

    internal constructor(ioPoolSize: Int) {
        this.config = null
        IO_EXECUTOR = IOExecutor(ioPoolSize)
        singleThreadExecutorTag = UrlHashGenerator.hashWithTsSeed()
    }

    internal constructor() {
        this.config = null
        IO_EXECUTOR = IOExecutor()
        singleThreadExecutorTag = UrlHashGenerator.hashWithTsSeed()
    }

    /**
     * Use this task when you want to offload some background task
     * @param TResult
     * @return
     */
    open fun <TResult> ioTask(): Task<TResult> {
        return taskOnExecutorWithName(IO_EXECUTOR, DEFAULT_CALLBACK_EXECUTOR, "ioTask")
    }

    /**
     * Use this task when you want to offload some background task
     * @param TResult
     * @return
     */
    open fun <TResult> ioTaskNonUi(): Task<TResult> {
        return taskOnExecutorWithName(IO_EXECUTOR, IO_EXECUTOR, "ioTaskNonUi")
    }

    /**
     * Use this task when you want to offload some background task and receive callback on current thread
     * @param TResult
     * @return
     */
    open fun <TResult> ioTaskWithCallbackOnCurrentThread(): Task<TResult> {
        return taskOnExecutorWithName(IO_EXECUTOR, CURRENT_THREAD_EXECUTOR, "ioTaskWithCallbackOnCurrentThread")
    }

    /**
     * Use this task to execute a runnable to main thread
     * @param TResult
     * @return
     */
    open fun <TResult> mainTask(): Task<TResult> {
        return taskOnExecutorWithName(MAIN_EXECUTOR, DEFAULT_CALLBACK_EXECUTOR, "Main")
    }

    /**
     * Use this task to execute a job in a sequential fashion for a particular feature
     * @param featureTag - name of the feature. e.g we have separate single pool executor for InApps & Geofences etc.
     * @param TResult
     * @return
     */
    open fun <TResult> postAsyncSafelyTask(featureTag: String?): Task<TResult> {
        if (featureTag == null) {
            throw IllegalArgumentException("Tag can't be null")
        }
        var postAsyncSafelyExecutor = postAsyncSafelyTasks[featureTag]

        if (postAsyncSafelyExecutor == null) {
            postAsyncSafelyExecutor = PostAsyncSafelyExecutor()
            postAsyncSafelyTasks[featureTag] = postAsyncSafelyExecutor
        }
        return taskOnExecutorWithName(postAsyncSafelyExecutor, DEFAULT_CALLBACK_EXECUTOR, "PostAsyncSafely")
    }

    /**
     * Common single thread pool for a particular account.
     * Use this for general purpose single pipe-lining of jobs.
     * @param TResult
     * @return
     */
    open fun <TResult> postAsyncSafelyTask(): Task<TResult> {
        val key: String? = if (config != null) {
            config.accountId
        } else {
            singleThreadExecutorTag
        }
        return postAsyncSafelyTask(key)
    }

    /**
     * Use this task to use your own custom executor for executing jobs & getting callbacks on default executors
     * @param taskExecutor - executor on which the provided task will be executed
     * @param taskName - custom name for the task (e.g we have main, iotask , postasycnsafely)
     * @param TResult
     * @return - task
     */
    open fun <TResult> taskOnExecutor(taskExecutor: Executor?, taskName: String?): Task<TResult> {
        return taskOnExecutorWithName(taskExecutor, DEFAULT_CALLBACK_EXECUTOR, taskName)
    }

    /**
     * Use this task to use your own custom executor for executing jobs & getting callbacks on the provided callback executors
     * @param taskExecutor - executor on which the provided task will be executed
     * @param callbackExecutor - executor on which the callbacks will be executed
     * @param taskName - custom name for the task (e.g we have main, iotask , postasycnsafely)
     * @param TResult
     * @return - task
     */
    open fun <TResult> taskOnExecutorWithName(
        taskExecutor: Executor?,
        callbackExecutor: Executor?, taskName: String?
    ): Task<TResult> {
        if (taskExecutor == null || callbackExecutor == null) {
            throw IllegalArgumentException(
                "Can't create task "
                        + taskName + " with null executors"
            )
        }
        return Task(config, taskExecutor, callbackExecutor, taskName)
    }
}
