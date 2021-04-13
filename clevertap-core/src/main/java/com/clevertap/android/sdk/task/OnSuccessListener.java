package com.clevertap.android.sdk.task;

/**
 * Interface to provide success callback
 * @param <TResult>
 */
public interface OnSuccessListener<TResult> {
    void onSuccess(TResult result);
}