package com.clevertap.android.sdk.task

import androidx.annotation.RestrictTo
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Executor service pool containing more threads to offload the IO tasks.
 *
 * Public + RestrictTo (not internal): exposed through the public `CTExecutors.IO_EXECUTOR` field.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class IOExecutor : ExecutorService {

    private val numCores = Runtime.getRuntime().availableProcessors()

    var executorService: ExecutorService

    constructor() {
        executorService = ThreadPoolExecutor(
            numCores * 2, numCores * 2,
            60L, TimeUnit.SECONDS, LinkedBlockingQueue()
        )
    }

    constructor(poolSize: Int) {
        executorService = ThreadPoolExecutor(
            4,
            poolSize,
            60L,
            TimeUnit.SECONDS,
            LinkedBlockingQueue()
        )
    }

    @Throws(InterruptedException::class)
    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
        return executorService.awaitTermination(timeout, unit)
    }

    override fun execute(command: Runnable) {
        executorService.execute(command)
    }

    @Throws(InterruptedException::class)
    override fun <T> invokeAll(tasks: MutableCollection<out Callable<T>>): MutableList<Future<T>> {
        return executorService.invokeAll(tasks)
    }

    @Throws(InterruptedException::class)
    override fun <T> invokeAll(
        tasks: MutableCollection<out Callable<T>>,
        timeout: Long,
        unit: TimeUnit
    ): MutableList<Future<T>> {
        return executorService.invokeAll(tasks, timeout, unit)
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    override fun <T> invokeAny(tasks: MutableCollection<out Callable<T>>): T {
        return executorService.invokeAny(tasks)
    }

    @Throws(ExecutionException::class, InterruptedException::class, TimeoutException::class)
    override fun <T> invokeAny(tasks: MutableCollection<out Callable<T>>, timeout: Long, unit: TimeUnit): T {
        return executorService.invokeAny(tasks, timeout, unit)
    }

    override fun isShutdown(): Boolean {
        return executorService.isShutdown
    }

    override fun isTerminated(): Boolean {
        return executorService.isTerminated
    }

    override fun shutdown() {
        executorService.shutdown()
    }

    override fun shutdownNow(): MutableList<Runnable> {
        return executorService.shutdownNow()
    }

    override fun <T> submit(task: Callable<T>): Future<T> {
        return executorService.submit(task)
    }

    override fun <T> submit(task: Runnable, result: T): Future<T> {
        return executorService.submit(task, result)
    }

    override fun submit(task: Runnable): Future<*> {
        return executorService.submit(task)
    }
}
