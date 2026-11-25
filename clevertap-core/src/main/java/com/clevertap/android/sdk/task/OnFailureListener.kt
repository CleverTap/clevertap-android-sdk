package com.clevertap.android.sdk.task

import androidx.annotation.RestrictTo

/**
 * Interface to provide failure callbacks
 * @param TResult
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface OnFailureListener<TResult> {

    fun onFailure(result: TResult)
}
