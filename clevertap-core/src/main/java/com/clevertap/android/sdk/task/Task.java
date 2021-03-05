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

public class Task<TResult> {

    protected enum STATE {FAILED, SUCCESS, READY_TO_RUN, RUNNING}

    protected final CleverTapInstanceConfig mConfig;

    protected final Executor mDefaultCallbackExecutor;

    protected final Executor mExecutor;

    protected final List<FailureExecutable<Exception>> mFailureExecutables = new ArrayList<>();

    protected TResult mResult;

    protected final List<SuccessExecutable<TResult>> mSuccessExecutables = new ArrayList<>();

    protected STATE mTaskState = STATE.READY_TO_RUN;
    private final String mTaskName;

    Task(final CleverTapInstanceConfig config, Executor executor,
            final Executor defaultCallbackExecutor, final String taskName) {
        mExecutor = executor;
        mDefaultCallbackExecutor = defaultCallbackExecutor;
        mConfig = config;
        mTaskName = taskName;
    }

    @NonNull
    public synchronized Task<TResult> addOnFailureListener(@NonNull final Executor executor,
            final OnFailureListener<Exception> listener) {
        if (listener != null) {
            mFailureExecutables.add(new FailureExecutable<>(executor, listener));
        }
        return this;
    }

    @NonNull
    public Task<TResult> addOnFailureListener(@NonNull OnFailureListener<Exception> listener) {
        return addOnFailureListener(mDefaultCallbackExecutor, listener);
    }

    @NonNull
    public Task<TResult> addOnSuccessListener(@NonNull final Executor executor,
            final OnSuccessListener<TResult> listener) {
        if (listener != null) {
            mSuccessExecutables.add(new SuccessExecutable<>(executor, listener, mConfig));
        }
        return this;
    }

    @NonNull
    public Task<TResult> addOnSuccessListener(@NonNull OnSuccessListener<TResult> listener) {
        return addOnSuccessListener(mDefaultCallbackExecutor, listener);
    }

    public void execute(final String logTag, final Callable<TResult> callable) {
        mExecutor.execute(newRunnableForTask(logTag, callable));
    }

    public boolean isSuccess() {
        return mTaskState == STATE.SUCCESS;
    }

    @NonNull
    public Task<TResult> removeOnFailureListener(@NonNull OnFailureListener<Exception> listener) {
        Iterator<FailureExecutable<Exception>> iterator = mFailureExecutables.iterator();
        while (iterator.hasNext()) {
            FailureExecutable<Exception> item = iterator.next();
            if (item.getFailureListener() == listener) {
                iterator.remove();
            }
        }
        return this;
    }

    @NonNull
    public Task<TResult> removeOnSuccessListener(@NonNull OnSuccessListener<TResult> listener) {
        Iterator<SuccessExecutable<TResult>> iterator = mSuccessExecutables.iterator();
        while (iterator.hasNext()) {
            SuccessExecutable<TResult> item = iterator.next();
            if (item.getSuccessListener() == listener) {
                iterator.remove();
            }
        }
        return this;
    }

    public Future<?> submit(final String logTag, final Callable<TResult> callable) {
        if (!(mExecutor instanceof ExecutorService)) {
            throw new UnsupportedOperationException(
                    "Can't use this method without ExecutorService, Use Execute alternatively ");
        }
        return ((ExecutorService) mExecutor).submit(newRunnableForTask(logTag, callable));
    }

    void onFailure(final Exception e) {
        setState(STATE.FAILED);
        for (Executable<Exception> failureExecutable : mFailureExecutables) {
            failureExecutable.execute(e);
        }
    }

    void onSuccess(final TResult result) {
        setState(STATE.SUCCESS);
        setResult(result);
        for (Executable<TResult> successExecutable : mSuccessExecutables) {
            successExecutable.execute(mResult);
        }
    }

    void setResult(final TResult result) {
        mResult = result;
    }

    void setState(final STATE taskState) {
        mTaskState = taskState;
    }

    private Runnable newRunnableForTask(final String logTag, final Callable<TResult> callable) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    mConfig.getLogger()
                            .verbose(mTaskName+ " Task: " + logTag + " starting on..." + Thread.currentThread().getName());
                    TResult result = callable.call();
                    mConfig.getLogger().verbose(
                            mTaskName+ " Task: " + logTag + " executed successfully on..." + Thread.currentThread().getName());
                    onSuccess(result);
                } catch (Exception e) {
                    onFailure(e);
                    mConfig.getLogger().verbose(
                            mTaskName+ " Task: " + logTag + " failed to execute on..." + Thread.currentThread().getName(), e);
                    e.printStackTrace();
                }
            }
        };
    }
}