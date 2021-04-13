package com.clevertap.android.sdk.task;

import androidx.annotation.NonNull;

import com.clevertap.android.sdk.CleverTapInstanceConfig;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Definition of task is to execute some work & return success or failure callbacks
 *
 * @param <TResult>
 */
public class Task<TResult> {

    protected enum STATE {FAILED, SUCCESS, READY_TO_RUN, RUNNING}

    protected final CleverTapInstanceConfig config;

    protected final Executor defaultCallbackExecutor;

    protected final Executor executor;

    protected final List<FailureExecutable<Exception>> failureExecutables = new ArrayList<>();

    protected TResult result;

    protected final List<SuccessExecutable<TResult>> successExecutables = new ArrayList<>();

    protected STATE taskState = STATE.READY_TO_RUN;
    private final String taskName;

    Task(final CleverTapInstanceConfig config, Executor executor,
         final Executor defaultCallbackExecutor, final String taskName) {
        this.executor = executor;
        this.defaultCallbackExecutor = defaultCallbackExecutor;
        this.config = config;
        this.taskName = taskName;
    }

    /**
     * Register listener to get failure callbacks on the provided executor
     *
     * @param executor  - executor on which the failure callback will be called
     * @param listener- failure listener
     * @return task
     */
    @NonNull
    public synchronized Task<TResult> addOnFailureListener(@NonNull final Executor executor,
                                                           final OnFailureListener<Exception> listener) {
        if (listener != null) {
            failureExecutables.add(new FailureExecutable<>(executor, listener));
        }
        return this;
    }

    /**
     * Register listener to get failure callbacks on main thread
     *
     * @param listener- failure listener
     * @return task
     */
    @NonNull
    public Task<TResult> addOnFailureListener(@NonNull OnFailureListener<Exception> listener) {
        return addOnFailureListener(defaultCallbackExecutor, listener);
    }

    /**
     * Register listener to get success callbacks on the provided executor
     *
     * @param executor  - executor on which the success callback will be called
     * @param listener- success listener
     * @return task
     */
    @NonNull
    public Task<TResult> addOnSuccessListener(@NonNull final Executor executor,
                                              final OnSuccessListener<TResult> listener) {
        if (listener != null) {
            successExecutables.add(new SuccessExecutable<>(executor, listener, config));
        }
        return this;
    }

    /**
     * Register listener to get success callbacks on main thread
     *
     * @param listener- success listener
     * @return task
     */
    @NonNull
    public Task<TResult> addOnSuccessListener(@NonNull OnSuccessListener<TResult> listener) {
        return addOnSuccessListener(defaultCallbackExecutor, listener);
    }

    /**
     * Simple method to execute the task
     *
     * @param logTag   - tag name to identify the task state in logs.
     * @param callable - piece of code to run
     */
    public void execute(final String logTag, final Callable<TResult> callable) {
        executor.execute(newRunnableForTask(logTag, callable));
    }

    /**
     * Returns the state of task
     * Ref{@link STATE}
     *
     * @return
     */
    public boolean isSuccess() {
        return taskState == STATE.SUCCESS;
    }

    /***
     * Removes the failure listener from the task.
     * @param listener - failure listener
     * @return task
     */
    @SuppressWarnings("unused")
    @NonNull
    public Task<TResult> removeOnFailureListener(@NonNull OnFailureListener<Exception> listener) {
        Iterator<FailureExecutable<Exception>> iterator = failureExecutables.iterator();
        while (iterator.hasNext()) {
            FailureExecutable<Exception> item = iterator.next();
            if (item.getFailureListener() == listener) {
                iterator.remove();
            }
        }
        return this;
    }

    /***
     * Removes the Success listener from the task.
     * @param listener - success listener
     * @return task
     */

    @SuppressWarnings("unused")
    @NonNull
    public Task<TResult> removeOnSuccessListener(@NonNull OnSuccessListener<TResult> listener) {
        Iterator<SuccessExecutable<TResult>> iterator = successExecutables.iterator();
        while (iterator.hasNext()) {
            SuccessExecutable<TResult> item = iterator.next();
            if (item.getSuccessListener() == listener) {
                iterator.remove();
            }
        }
        return this;
    }

    /**
     * Use this method in-case we need future task for the execution
     *
     * @param logTag   - tag name to identify the task state in logs.
     * @param callable - piece of code to run
     */
    public Future<?> submit(final String logTag, final Callable<TResult> callable) {
        if (!(executor instanceof ExecutorService)) {
            throw new UnsupportedOperationException(
                    "Can't use this method without ExecutorService, Use Execute alternatively ");
        }
        return ((ExecutorService) executor).submit(newRunnableForTask(logTag, callable));
    }

    void onFailure(final Exception e) {
        setState(STATE.FAILED);
        for (Executable<Exception> failureExecutable : failureExecutables) {
            failureExecutable.execute(e);
        }
    }

    void onSuccess(final TResult result) {
        setState(STATE.SUCCESS);
        setResult(result);
        for (Executable<TResult> successExecutable : successExecutables) {
            successExecutable.execute(this.result);
        }
    }

    void setResult(final TResult result) {
        this.result = result;
    }

    void setState(final STATE taskState) {
        this.taskState = taskState;
    }

    /**
     * Wraps the provided piece of code in runnable to execute on executor.
     *
     * @param logTag   - tag with which this task can be identified in the logs.
     * @param callable - piece of code to run.
     * @return Runnable
     */
    private Runnable newRunnableForTask(final String logTag, final Callable<TResult> callable) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    config.getLogger()
                            .verbose(taskName + " Task: " + logTag + " starting on..." + Thread.currentThread().getName());
                    TResult result = callable.call();
                    config.getLogger().verbose(
                            taskName + " Task: " + logTag + " executed successfully on..." + Thread.currentThread().getName());
                    onSuccess(result);
                } catch (Exception e) {
                    onFailure(e);
                    config.getLogger().verbose(
                            taskName + " Task: " + logTag + " failed to execute on..." + Thread.currentThread().getName(), e);
                    e.printStackTrace();
                }
            }
        };
    }
}