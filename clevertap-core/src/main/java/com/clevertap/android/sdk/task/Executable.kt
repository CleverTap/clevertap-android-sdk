package com.clevertap.android.sdk.task

import java.util.concurrent.Executor
import androidx.annotation.RestrictTo

/**
 * Stuffs which can be executed on an executor with certain input parameter
 * @param TResult
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal abstract class Executable<TResult>(protected val executor: Executor) {

    abstract fun execute(input: TResult)
}
