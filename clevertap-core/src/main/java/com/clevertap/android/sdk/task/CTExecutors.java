package com.clevertap.android.sdk.task;

import androidx.annotation.RestrictTo;
import com.clevertap.android.sdk.CleverTapInstanceConfig;
import java.util.HashMap;
import java.util.concurrent.Executor;

/**
 * Global executor pools for the whole application.
 * <p>
 * Grouping tasks like this avoids the effects of task starvation (e.g. disk reads don't wait behind
 * webservice requests).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CTExecutors {

    public final IOExecutor IO_EXECUTOR = new IOExecutor();

    public final MainThreadExecutor MAIN_EXECUTOR = new MainThreadExecutor();

    public final MainThreadExecutor DEFAULT_CALLBACK_EXECUTOR = MAIN_EXECUTOR;

    protected final CleverTapInstanceConfig config;

    private final HashMap<String, PostAsyncSafelyExecutor> postAsyncSafelyTasks = new HashMap<>();

    CTExecutors(CleverTapInstanceConfig config) {
        this.config = config;
    }

    public <TResult> Task<TResult> ioTask() {
        return taskOnExecutorWithName(IO_EXECUTOR, DEFAULT_CALLBACK_EXECUTOR, "ioTask");
    }

    public <TResult> Task<TResult> mainTask() {
        return taskOnExecutorWithName(MAIN_EXECUTOR, DEFAULT_CALLBACK_EXECUTOR, "Main");
    }

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

    public <TResult> Task<TResult> postAsyncSafelyTask() {
        return postAsyncSafelyTask(config.getAccountId());
    }

    public <TResult> Task<TResult> taskOnExecutor(Executor taskExecutor, String taskName) {
        return taskOnExecutorWithName(taskExecutor, DEFAULT_CALLBACK_EXECUTOR, taskName);
    }

    public <TResult> Task<TResult> taskOnExecutorWithName(Executor taskExecutor,
            Executor callbackExecutor, String taskName) {
        if (taskExecutor == null || callbackExecutor == null) {
            throw new IllegalArgumentException("Can't create task "
                    + taskName + " with null executors");
        }
        return new Task<>(config, taskExecutor, callbackExecutor, taskName);
    }
}