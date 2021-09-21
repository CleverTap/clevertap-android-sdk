package com.clevertap.android.pushtemplates

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal class AsyncHelper private constructor() {
    private var EXECUTOR_THREAD_ID: Long = 0
    private val es: ExecutorService
    fun postAsyncSafely(name: String, runnable: Runnable) {
        try {
            val executeSync = Thread.currentThread().id == EXECUTOR_THREAD_ID
            if (executeSync) {
                runnable.run()
            } else {
                es.submit {
                    EXECUTOR_THREAD_ID = Thread.currentThread().id
                    try {
                        runnable.run()
                    } catch (t: Throwable) {
                        PTLog.verbose("Executor service: Failed to complete the scheduled task$name")
                    }
                }
            }
        } catch (t: Throwable) {
            PTLog.verbose("Failed to submit task to the executor service")
        }
    }

    companion object {
        private var asyncHelperInstance: AsyncHelper? = null
        @JvmStatic
        val instance: AsyncHelper?
            get() {
                if (asyncHelperInstance == null) {
                    asyncHelperInstance = AsyncHelper()
                }
                return asyncHelperInstance
            }
        @JvmStatic
        val mainThreadHandler: Handler
            get() {
                val mainThreadHandler: Handler
                mainThreadHandler = Handler(Looper.getMainLooper())
                return mainThreadHandler
            }
    }

    init {
        es = Executors.newFixedThreadPool(1)
    }
}