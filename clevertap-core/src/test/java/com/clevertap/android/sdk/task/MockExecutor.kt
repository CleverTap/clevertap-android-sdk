package com.clevertap.android.sdk.task

import java.util.concurrent.Executor

class MockExecutor:Executor {

    override fun execute(command: Runnable?) {
        command?.run()
    }
}