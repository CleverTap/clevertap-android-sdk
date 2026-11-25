package com.clevertap.android.sdk.task

import java.util.concurrent.Executor

/**
 * Stuffs which can be executed on an executor with certain input parameter
 * @param TResult
 */
internal abstract class Executable<TResult>(protected val executor: Executor) {

    abstract fun execute(input: TResult)
}
