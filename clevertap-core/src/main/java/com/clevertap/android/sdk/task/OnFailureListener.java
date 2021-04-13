package com.clevertap.android.sdk.task;

/**
 * Interface to provide failure callbacks
 * @param <TResult>
 */
public interface OnFailureListener<TResult> {

    void onFailure(TResult result);
}