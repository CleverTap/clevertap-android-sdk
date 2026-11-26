package com.clevertap.android.sdk.task

import java.util.concurrent.Executor
import androidx.annotation.RestrictTo

/**
 * Wrapper class to execute runnable after a task is successful.
 * Ref: [OnSuccessListener]
 *
 * @param TResult
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class SuccessExecutable<TResult>(
    executor: Executor,
    val successListener: OnSuccessListener<TResult>
) : Executable<TResult>(executor) {

    override fun execute(input: TResult) {
        executor.execute { successListener.onSuccess(input) }
    }
}
