package com.clevertap.android.sdk.task

import android.os.Handler
import android.os.Looper
import androidx.annotation.RestrictTo
import java.util.concurrent.Executor

/**
 * Executor service to delegate runnables to Main Thread.
 *
 * Public + RestrictTo (not internal): exposed through the public `CTExecutors.MAIN_EXECUTOR` field.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
open class MainThreadExecutor : Executor {

    var mainThreadHandler: Handler = Handler(Looper.getMainLooper())

    override fun execute(command: Runnable) {
        mainThreadHandler.post(command)
    }
}
