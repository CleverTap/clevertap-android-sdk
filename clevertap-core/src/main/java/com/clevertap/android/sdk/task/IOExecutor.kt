package com.clevertap.android.sdk.task

import androidx.annotation.RestrictTo
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Executor service pool containing more threads to offload the IO tasks.
 *
 * Every [ExecutorService] call is delegated to [executorService]: by default a fixed pool of twice the
 * number of cores; [CTExecutors] created with an explicit IO pool size pass [threadPool] (4, size).
 *
 * Public + RestrictTo (not internal): exposed through the public `CTExecutors.IO_EXECUTOR` field.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class IOExecutor internal constructor(
    internal val executorService: ExecutorService = threadPool(
        corePoolSize = Runtime.getRuntime().availableProcessors() * 2,
        maximumPoolSize = Runtime.getRuntime().availableProcessors() * 2
    )
) : ExecutorService by executorService {

    internal companion object {
        fun threadPool(corePoolSize: Int, maximumPoolSize: Int): ExecutorService =
            ThreadPoolExecutor(corePoolSize, maximumPoolSize, 60L, TimeUnit.SECONDS, LinkedBlockingQueue())
    }
}
