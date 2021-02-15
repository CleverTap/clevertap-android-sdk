package com.clevertap.android.sdk.task;

public interface OnFailureListener<TResult> {

    void onFailure(TResult result);
}