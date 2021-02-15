package com.clevertap.android.sdk.task;

import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

public class Task<TResult> {

    protected enum STATE {FAILED, SUCCESS, READY_TO_RUN, RUNNING}

    protected final Executor mDefaultCallbackExecutor;

    protected final Executor mExecutor;

    protected final List<Executable<Exception>> mFailureExecutables = new ArrayList<>();

    protected TResult mResult;

    protected final List<Executable<TResult>> mSuccessExecutables = new ArrayList<>();

    protected STATE mTaskState = STATE.READY_TO_RUN;

    Task(Executor executor, final Executor defaultCallbackExecutor) {
        mExecutor = executor;
        mDefaultCallbackExecutor = defaultCallbackExecutor;
    }

    @NonNull
    public Task<TResult> addOnFailureListener(@NonNull final Executor executor,
            @NonNull final OnFailureListener<Exception> listener) {
        mFailureExecutables.add(new FailureExecutable<Exception>(executor, listener));
        return this;
    }

    @NonNull
    public Task<TResult> addOnFailureListener(@NonNull OnFailureListener<Exception> listener) {
        return addOnFailureListener(mDefaultCallbackExecutor, listener);
    }

    @NonNull
    public Task<TResult> addOnSuccessListener(@NonNull final Executor executor,
            @NonNull final OnSuccessListener<TResult> listener) {
        mSuccessExecutables.add(new SuccessExecutable<TResult>(executor, listener));
        return this;
    }

    @NonNull
    public Task<TResult> addOnSuccessListener(@NonNull OnSuccessListener<TResult> listener) {
        return addOnSuccessListener(mDefaultCallbackExecutor, listener);
    }

    public void call(final Callable<TResult> callable) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    TResult result = callable.call();
                    onSuccess(result);
                } catch (Exception e) {
                    onFailure(e);
                    e.printStackTrace();
                }
            }
        });
    }

    public boolean isSuccess() {
        return mTaskState == STATE.SUCCESS;
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
}