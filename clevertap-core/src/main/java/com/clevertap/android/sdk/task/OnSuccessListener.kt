package com.clevertap.android.sdk.task

import androidx.annotation.RestrictTo

/**
 * Interface to provide success callback
 * @param TResult
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface OnSuccessListener<TResult> {
    public fun onSuccess(result: TResult)
}
