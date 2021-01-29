package com.clevertap.android.sdk

import org.mockito.*
import java.util.concurrent.Future

internal class MockPostAsyncSafelyHandler(config: CleverTapInstanceConfig?) :
    PostAsyncSafelyHandler(config) {

    var future: Future<*>? = null
    override fun postAsyncSafely(
        name: String,
        runnable: Runnable
    ): Future<*>? {
        runnable.run()
        return future
    }

    override fun runOnNotificationQueue(runnable: Runnable) {
        runnable.run()
    }

    init {
        future = Mockito.mock(Future::class.java)
    }
}