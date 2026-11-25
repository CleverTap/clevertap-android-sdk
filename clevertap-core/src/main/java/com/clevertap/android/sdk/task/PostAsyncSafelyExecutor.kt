package com.clevertap.android.sdk.task

import com.clevertap.android.sdk.Logger
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.RunnableFuture
import java.util.concurrent.TimeUnit

/**
 * Post async safely executor is nothing but a single thread pool executor
 * It's a single pipeline to execute task sequentially
 */
internal class PostAsyncSafelyExecutor : ExecutorService {

    private var EXECUTOR_THREAD_ID: Long = 0

    var executor: ExecutorService = Executors.newSingleThreadExecutor()

    @Throws(InterruptedException::class)
    override fun awaitTermination(timeout: Long, unit: TimeUnit): Boolean {
        return executor.awaitTermination(timeout, unit)
    }

    override fun execute(task: Runnable?) {
        if (task == null) {
            throw NullPointerException("PostAsyncSafelyExecutor#execute: task can't ne null")
        }
        val executeSync = Thread.currentThread().id == EXECUTOR_THREAD_ID
        if (executeSync) {
            task.run()
        } else {
            executor.execute {
                EXECUTOR_THREAD_ID = Thread.currentThread().id
                task.run()
            }
        }
    }

    @Throws(UnsupportedOperationException::class)
    override fun <T> invokeAll(tasks: MutableCollection<out Callable<T>>?): MutableList<Future<T>> {
        throw UnsupportedOperationException("PostAsyncSafelyExecutor#invokeAll: This method is not supported")
    }

    @Throws(UnsupportedOperationException::class)
    override fun <T> invokeAll(
        tasks: MutableCollection<out Callable<T>>?,
        timeout: Long,
        unit: TimeUnit?
    ): MutableList<Future<T>> {
        throw UnsupportedOperationException("PostAsyncSafelyExecutor#invokeAll: This method is not supported")
    }

    @Throws(UnsupportedOperationException::class)
    override fun <T> invokeAny(tasks: MutableCollection<out Callable<T>>?): T {
        throw UnsupportedOperationException("PostAsyncSafelyExecutor#invokeAny: This method is not supported")
    }

    @Throws(UnsupportedOperationException::class)
    override fun <T> invokeAny(tasks: MutableCollection<out Callable<T>>?, timeout: Long, unit: TimeUnit?): T {
        throw UnsupportedOperationException("PostAsyncSafelyExecutor#invokeAny: This method is not supported")
    }

    override fun isShutdown(): Boolean {
        return executor.isShutdown
    }

    override fun isTerminated(): Boolean {
        return executor.isTerminated
    }

    override fun shutdown() {
        executor.shutdown()
    }

    override fun shutdownNow(): MutableList<Runnable> {
        return executor.shutdownNow()
    }

    override fun <T> submit(task: Callable<T>?): Future<T>? {
        if (task == null) {
            throw NullPointerException("PostAsyncSafelyExecutor#submit: task can't ne null")
        }
        var future: Future<T>? = null
        val executeSync = Thread.currentThread().id == EXECUTOR_THREAD_ID
        if (executeSync) {
            try {
                task.call()
            } catch (e: Exception) {
                Logger.d(TAG, "Error executing task synchronously", e)
            }
        } else {
            future = executor.submit(Callable {
                EXECUTOR_THREAD_ID = Thread.currentThread().id
                task.call()
            })
        }
        return future
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
