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
public open class CTExecutors {

    @JvmField
    public val IO_EXECUTOR: IOExecutor

    @JvmField
    public val MAIN_EXECUTOR: MainThreadExecutor = MainThreadExecutor()

    @JvmField
    public val CURRENT_THREAD_EXECUTOR: Executor = Executor { it.run() }

    @JvmField
    public val DEFAULT_CALLBACK_EXECUTOR: MainThreadExecutor = MAIN_EXECUTOR

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
        IO_EXECUTOR = IOExecutor(IOExecutor.threadPool(corePoolSize = 4, maximumPoolSize = ioPoolSize))
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
    public open fun <TResult> ioTask(): Task<TResult> =
        taskOnExecutorWithName(IO_EXECUTOR, DEFAULT_CALLBACK_EXECUTOR, "ioTask")

    /**
     * Use this task when you want to offload some background task
     * @param TResult
     * @return
     */
    public open fun <TResult> ioTaskNonUi(): Task<TResult> =
        taskOnExecutorWithName(IO_EXECUTOR, IO_EXECUTOR, "ioTaskNonUi")

    /**
     * Use this task when you want to offload some background task and receive callback on current thread
     * @param TResult
     * @return
     */
    public open fun <TResult> ioTaskWithCallbackOnCurrentThread(): Task<TResult> =
        taskOnExecutorWithName(IO_EXECUTOR, CURRENT_THREAD_EXECUTOR, "ioTaskWithCallbackOnCurrentThread")

    /**
     * Use this task to execute a runnable to main thread
     * @param TResult
     * @return
     */
    public open fun <TResult> mainTask(): Task<TResult> =
        taskOnExecutorWithName(MAIN_EXECUTOR, DEFAULT_CALLBACK_EXECUTOR, "Main")

    /**
     * Use this task to execute a job in a sequential fashion for a particular feature
     * @param featureTag - name of the feature. e.g we have separate single pool executor for InApps & Geofences etc.
     * @param TResult
     * @return
     */
    public open fun <TResult> postAsyncSafelyTask(featureTag: String?): Task<TResult> {
        requireNotNull(featureTag) { "Tag can't be null" }
        val postAsyncSafelyExecutor = postAsyncSafelyTasks.getOrPut(featureTag) { PostAsyncSafelyExecutor() }
        return taskOnExecutorWithName(postAsyncSafelyExecutor, DEFAULT_CALLBACK_EXECUTOR, "PostAsyncSafely")
    }

    /**
     * Common single thread pool for a particular account.
     * Use this for general purpose single pipe-lining of jobs.
     * @param TResult
     * @return
     */
    public open fun <TResult> postAsyncSafelyTask(): Task<TResult> =
        postAsyncSafelyTask(if (config != null) config.accountId else singleThreadExecutorTag)

    /**
     * Use this task to use your own custom executor for executing jobs & getting callbacks on default executors
     * @param taskExecutor - executor on which the provided task will be executed
     * @param taskName - custom name for the task (e.g we have main, iotask , postasycnsafely)
     * @param TResult
     * @return - task
     */
    public open fun <TResult> taskOnExecutor(taskExecutor: Executor?, taskName: String?): Task<TResult> =
        taskOnExecutorWithName(taskExecutor, DEFAULT_CALLBACK_EXECUTOR, taskName)

    /**
     * Use this task to use your own custom executor for executing jobs & getting callbacks on the provided callback executors
     * @param taskExecutor - executor on which the provided task will be executed
     * @param callbackExecutor - executor on which the callbacks will be executed
     * @param taskName - custom name for the task (e.g we have main, iotask , postasycnsafely)
     * @param TResult
     * @return - task
     */
    public open fun <TResult> taskOnExecutorWithName(
        taskExecutor: Executor?,
        callbackExecutor: Executor?,
        taskName: String?
    ): Task<TResult> {
        require(taskExecutor != null && callbackExecutor != null) {
            "Can't create task $taskName with null executors"
        }
        return Task(config, taskExecutor, callbackExecutor, taskName)
    }
}
