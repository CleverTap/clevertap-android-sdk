package com.clevertap.android.sdk.task

import com.clevertap.android.sdk.Logger
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.RunnableFuture
import java.util.concurrent.TimeUnit
import androidx.annotation.RestrictTo

/**
 * Post async safely executor is nothing but a single thread pool executor
 * It's a single pipeline to execute task sequentially.
 *
 * A task submitted from the executor's own thread runs synchronously (re-entrancy without deadlock).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PostAsyncSafelyExecutor(
    internal val executor: ExecutorService = Executors.newSingleThreadExecutor()
) : ExecutorService {

    private var executorThreadId: Long = 0

    private val isOnExecutorThread: Boolean
        get() = Thread.currentThread().id == executorThreadId

    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean = executor.awaitTermination(timeout, unit)

    override fun execute(task: Runnable?) {
        if (task == null) {
            throw NullPointerException("PostAsyncSafelyExecutor#execute: task can't ne null")
        }
        if (isOnExecutorThread) {
            task.run()
        } else {
            executor.execute {
                executorThreadId = Thread.currentThread().id
                task.run()
            }
        }
    }

    override fun <T> invokeAll(tasks: MutableCollection<out Callable<T>>?): MutableList<Future<T>> =
        throw UnsupportedOperationException("PostAsyncSafelyExecutor#invokeAll: This method is not supported")

    override fun <T> invokeAll(
        tasks: MutableCollection<out Callable<T>>?,
        timeout: Long,
        unit: TimeUnit?
    ): MutableList<Future<T>> =
        throw UnsupportedOperationException("PostAsyncSafelyExecutor#invokeAll: This method is not supported")

    override fun <T> invokeAny(tasks: MutableCollection<out Callable<T>>?): T =
        throw UnsupportedOperationException("PostAsyncSafelyExecutor#invokeAny: This method is not supported")

    override fun <T> invokeAny(tasks: MutableCollection<out Callable<T>>?, timeout: Long, unit: TimeUnit?): T =
        throw UnsupportedOperationException("PostAsyncSafelyExecutor#invokeAny: This method is not supported")

    override fun isShutdown(): Boolean = executor.isShutdown

    override fun isTerminated(): Boolean = executor.isTerminated

    override fun shutdown() = executor.shutdown()

    override fun shutdownNow(): MutableList<Runnable> = executor.shutdownNow()

    override fun <T> submit(task: Callable<T>?): Future<T>? {
        if (task == null) {
            throw NullPointerException("PostAsyncSafelyExecutor#submit: task can't ne null")
        }
        if (isOnExecutorThread) {
            try {
                task.call()
            } catch (e: Exception) {
                Logger.d(TAG, "Error executing task synchronously", e)
            }
            return null
        }
        return executor.submit(Callable {
            executorThreadId = Thread.currentThread().id
            task.call()
        })
    }

    override fun <T> submit(task: Runnable?, result: T): Future<T> {
        if (task == null) {
            throw NullPointerException("PostAsyncSafelyExecutor#submit: task can't ne null")
        }
        val futureTask: RunnableFuture<T> = FutureTask(task, result)
        execute(futureTask)
        return futureTask
    }

    override fun submit(task: Runnable?): Future<*> {
        if (task == null) {
            throw NullPointerException("PostAsyncSafelyExecutor#submit: task can't ne null")
        }
        val futureTask: RunnableFuture<Void?> = FutureTask(task, null)
        execute(futureTask)
        return futureTask
    }

    private companion object {
        private const val TAG = "PostAsyncSafelyExecutor"
    }
}
