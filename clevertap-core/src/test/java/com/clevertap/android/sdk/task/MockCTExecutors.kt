package com.clevertap.android.sdk.task

import com.clevertap.android.sdk.CleverTapInstanceConfig

class MockCTExecutors(config: CleverTapInstanceConfig) : CTExecutors(config) {

    override fun <TResult : Any?> ioTask(): Task<TResult> {
        val executor = MockExecutorService()
        return Task(config, executor, executor, "ioTask")
    }

    override fun <TResult : Any?> mainTask(): Task<TResult> {
        val executor = MockExecutor()
        return Task(config, executor, executor, "mainTask")
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