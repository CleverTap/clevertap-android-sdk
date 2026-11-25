package com.clevertap.android.sdk.task

import java.util.concurrent.Executor

/**
 * Wrapper class to execute runnable after a task is failed.
 * Ref[OnFailureListener]
 *
 * @param TResult
 */
internal class FailureExecutable<TResult>(
    executor: Executor,
    val failureListener: OnFailureListener<TResult>
) : Executable<TResult>(executor) {

    override fun execute(input: TResult) {
        executor.execute { failureListener.onFailure(input) }
    }
}
