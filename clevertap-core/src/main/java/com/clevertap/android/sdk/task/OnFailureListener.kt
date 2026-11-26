package com.clevertap.android.sdk.task

import androidx.annotation.RestrictTo

/**
 * Interface to provide failure callbacks
 * @param TResult
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun interface OnFailureListener<TResult> {

    public fun onFailure(result: TResult)
}
