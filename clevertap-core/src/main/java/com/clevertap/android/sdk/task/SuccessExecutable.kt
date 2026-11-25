package com.clevertap.android.sdk.task

import java.util.concurrent.Executor

/**
 * Wrapper class to execute runnable after a task is successful.
 * Ref: [OnSuccessListener]
 *
 * @param TResult
 */
internal class SuccessExecutable<TResult>(
    executor: Executor,
    val successListener: OnSuccessListener<TResult>
) : Executable<TResult>(executor) {

    override fun execute(input: TResult) {
        executor.execute { successListener.onSuccess(input) }
    }
}
