package com.clevertap.android.sdk.task

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit

class MockExecutorService:ExecutorService {

    override fun execute(command: Runnable?) {
        command?.run()
    }

    override fun shutdown() {
        throw UnsupportedOperationException("Not Supported")
    }

    override fun shutdownNow(): MutableList<Runnable> {
        throw UnsupportedOperationException("Not Supported")
    }

    override fun isShutdown(): Boolean {
        throw UnsupportedOperationException("Not Supported")
    }

    override fun isTerminated(): Boolean {
        throw UnsupportedOperationException("Not Supported")
    }

    override fun awaitTermination(timeout: Long, unit: TimeUnit?): Boolean {
        throw UnsupportedOperationException("Not Supported")
    }

    override fun <T : Any?> submit(task: Callable<T>?): Future<T> {
        val futureTask = FutureTask<T>(task)
        futureTask.run()
        return futureTask
    }

    override fun <T : Any?> submit(task: Runnable?, result: T): Future<T> {
        val futureTask = FutureTask<T>(task, result)
        futureTask.run()
        return futureTask
    }

    override fun submit(task: Runnable?): Future<*> {
        val futureTask = FutureTask<Void>(task, null)
        futureTask.run()
        return futureTask
    }

    override fun <T : Any?> invokeAll(tasks: MutableCollection<out Callable<T>>?): MutableList<Future<T>> {
        throw UnsupportedOperationException("Not Supported")
    }

    override fun <T : Any?> invokeAll(
        tasks: MutableCollection<out Callable<T>>?,
        timeout: Long,
        unit: TimeUnit?
    ): MutableList<Future<T>> {
        throw java.lang.UnsupportedOperationException("Not Supported")
    }

    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>?): T {
        throw java.lang.UnsupportedOperationException("Not Supported")
    }

    override fun <T : Any?> invokeAny(tasks: MutableCollection<out Callable<T>>?, timeout: Long, unit: TimeUnit?): T {
        throw java.lang.UnsupportedOperationException("Not Supported")
    }
}