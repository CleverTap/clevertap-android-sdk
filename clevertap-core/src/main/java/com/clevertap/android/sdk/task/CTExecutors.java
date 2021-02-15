package com.clevertap.android.sdk.task;

import androidx.annotation.RestrictTo;
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

    // single executor acts like {@link PostAsyncSafelyHandler }
    private final PostAsyncSafelyExecutor POST_ASYNC_SAFELY_EXECUTOR = new PostAsyncSafelyExecutor();


    CTExecutors() {
    }

    public <TResult> Task<TResult> ioTask() {
        return taskWithExecutor(IO_EXECUTOR, DEFAULT_CALLBACK_EXECUTOR);
    }

    public <TResult> Task<TResult> mainTask() {
        return taskWithExecutor(MAIN_EXECUTOR, DEFAULT_CALLBACK_EXECUTOR);
    }

    public <TResult> Task<TResult> postAsyncSafelyTask() {
        return taskWithExecutor(POST_ASYNC_SAFELY_EXECUTOR, DEFAULT_CALLBACK_EXECUTOR);
    }

    public <TResult> Task<TResult> taskWithExecutor(Executor taskExecutor, Executor callbackExecutor) {
        if (taskExecutor == null || callbackExecutor == null) {
            throw new IllegalArgumentException("Can't create task with null executors");
        }
        return new Task<>(taskExecutor, callbackExecutor);
    }

    public <TResult> Task<TResult> taskWithExecutor(Executor taskExecutor) {
        if (taskExecutor == null) {
            throw new IllegalArgumentException("Can't create task with null executors");
        }
        return new Task<>(taskExecutor, DEFAULT_CALLBACK_EXECUTOR);
    }
}