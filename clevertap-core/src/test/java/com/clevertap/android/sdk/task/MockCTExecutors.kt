package com.clevertap.android.sdk.task

import com.clevertap.android.sdk.CleverTapInstanceConfig
import java.util.concurrent.Executor

class MockCTExecutors @JvmOverloads constructor(config: CleverTapInstanceConfig? = null) : CTExecutors(config) {

    override fun <TResult : Any?> ioTask(): Task<TResult> {
        val executor = MockExecutorService()
        return Task(config, executor, executor, "ioTask")
    }

    /** When true, mainTask work waits like a posted main-thread message until [runPendingMainTasks]. */
    var deferMainTasks = false
    private val pendingMainTasks = mutableListOf<Runnable>()

    fun runPendingMainTasks() {
        while (pendingMainTasks.isNotEmpty()) {
            pendingMainTasks.removeAt(0).run()
        }
    }

    override fun <TResult : Any?> mainTask(): Task<TResult> {
        val executor = if (deferMainTasks) Executor { pendingMainTasks.add(it) } else MockExecutor()
        return Task(config, executor, executor, "mainTask")
    }

    override fun <TResult : Any?> ioTaskNonUi(): Task<TResult> {
        val executor = MockExecutorService()
        return Task(config, executor, executor, "ioTaskNonUi")
    }

    override fun <TResult> ioTaskWithCallbackOnCurrentThread(): Task<TResult> {
        val executor = MockExecutorService()
        return Task(config, executor, executor, "ioTaskWithCallbackOnCurrentThread")
    }

    override fun <TResult : Any?> postAsyncSafelyTask(): Task<TResult> {
        val executor = MockExecutorService()
        return Task(config, executor, executor, "postAsyncSafelyTask")
    }

    override fun <TResult : Any?> postAsyncSafelyTask(featureTask: String): Task<TResult> {
        val executor = MockExecutorService()
        return Task(config, executor, executor, "postAsyncSafelyTask")
    }
}