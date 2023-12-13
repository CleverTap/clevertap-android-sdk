package com.clevertap.android.sdk.task;

import androidx.annotation.RestrictTo;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import com.clevertap.android.sdk.utils.UrlHashGenerator;

import java.util.HashMap;
import java.util.concurrent.Executor;

/**
 * Global executor pools per account.
 * <p>
 * Grouping tasks like this avoids the effects of task starvation (e.g. disk reads don't wait behind
 * webservice requests).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CTExecutors {

    public final IOExecutor IO_EXECUTOR;

    public final MainThreadExecutor MAIN_EXECUTOR = new MainThreadExecutor();

    public final MainThreadExecutor DEFAULT_CALLBACK_EXECUTOR = MAIN_EXECUTOR;

    protected final CleverTapInstanceConfig config;

    protected String singleThreadExecutorTag;

    private final HashMap<String, PostAsyncSafelyExecutor> postAsyncSafelyTasks = new HashMap<>();

    CTExecutors(CleverTapInstanceConfig config) {
        this.config = config;
        IO_EXECUTOR = new IOExecutor();
    }

    CTExecutors(int ioPoolSize) {
        this.config = null;
        IO_EXECUTOR = new IOExecutor(ioPoolSize);
        singleThreadExecutorTag = UrlHashGenerator.INSTANCE.hashWithTsSeed();
    }

    CTExecutors() {
        this.config = null;
        IO_EXECUTOR = new IOExecutor();
        singleThreadExecutorTag = UrlHashGenerator.INSTANCE.hashWithTsSeed();
    }

    /**
     * Use this task when you want to offload some background task
     * @param <TResult>
     * @return
     */
    public <TResult> Task<TResult> ioTask() {
        return taskOnExecutorWithName(IO_EXECUTOR, DEFAULT_CALLBACK_EXECUTOR, "ioTask");
    }

    /**
     * Use this task when you want to offload some background task
     * @param <TResult>
     * @return
     */
    public <TResult> Task<TResult> ioTaskNonUi() {
        return taskOnExecutorWithName(IO_EXECUTOR, IO_EXECUTOR, "ioTaskNonUi");
    }

    /**
     * Use this task to execute a runnable to main thread
     * @param <TResult>
     * @return
     */

    public <TResult> Task<TResult> mainTask() {
        return taskOnExecutorWithName(MAIN_EXECUTOR, DEFAULT_CALLBACK_EXECUTOR, "Main");
    }

    /**
     * Use this task to execute a job in a sequential fashion for a particular feature
     * @param featureTag - name of the feature. e.g we have separate single pool executor for InApps & Geofences etc.
     * @param <TResult>
     * @return
     */
    public <TResult> Task<TResult> postAsyncSafelyTask(String featureTag) {
        if (featureTag == null) {
            throw new IllegalArgumentException("Tag can't be null");
        }
        PostAsyncSafelyExecutor postAsyncSafelyExecutor = postAsyncSafelyTasks.get(featureTag);

        if (postAsyncSafelyExecutor == null) {
            postAsyncSafelyExecutor = new PostAsyncSafelyExecutor();
            postAsyncSafelyTasks.put(featureTag, postAsyncSafelyExecutor);
        }
        return taskOnExecutorWithName(postAsyncSafelyExecutor, DEFAULT_CALLBACK_EXECUTOR, "PostAsyncSafely");
    }

    /**
     * Common single thread pool for a particular account.
     * Use this for general purpose single pipe-lining of jobs.
     * @param <TResult>
     * @return
     */
    public <TResult> Task<TResult> postAsyncSafelyTask() {
        String key;
        if (config != null) {
            key = config.getAccountId();
        } else {
            key = singleThreadExecutorTag;
        }
        return postAsyncSafelyTask(key);
    }

    /**
     * Use this task to use your own custom executor for executing jobs & getting callbacks on default executors
     * @param taskExecutor - executor on which the provided task will be executed
     * @param taskName - custom name for the task (e.g we have main, iotask , postasycnsafely)
     * @param <TResult>
     * @return - task
     */
    public <TResult> Task<TResult> taskOnExecutor(Executor taskExecutor, String taskName) {
        return taskOnExecutorWithName(taskExecutor, DEFAULT_CALLBACK_EXECUTOR, taskName);
    }

    /**
     * Use this task to use your own custom executor for executing jobs & getting callbacks on the provided callback executors
     * @param taskExecutor - executor on which the provided task will be executed
     * @param callbackExecutor - executor on which the callbacks will be executed
     * @param taskName - custom name for the task (e.g we have main, iotask , postasycnsafely)
     * @param <TResult>
     * @return - task
     */
    public <TResult> Task<TResult> taskOnExecutorWithName(Executor taskExecutor,
            Executor callbackExecutor, String taskName) {
        if (taskExecutor == null || callbackExecutor == null) {
            throw new IllegalArgumentException("Can't create task "
                    + taskName + " with null executors");
        }
        return new Task<>(config, taskExecutor, callbackExecutor, taskName);
    }
}