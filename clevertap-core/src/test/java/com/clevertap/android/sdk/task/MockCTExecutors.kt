package com.clevertap.android.sdk.task

class MockCTExecutors : CTExecutors() {

    override fun <TResult : Any?> ioTask(): Task<TResult> {
        val executor = MockExecutor()
        return Task(executor, executor)
    }

    override fun <TResult : Any?> mainTask(): Task<TResult> {
        val executor = MockExecutor()
        return Task(executor, executor)
    }

    override fun <TResult : Any?> postAsyncSafelyTask(): Task<TResult> {
        val executor = MockExecutor()
        return Task(executor, executor)
    }
}