package com.clevertap.android.sdk.task

import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS

@RunWith(RobolectricTestRunner::class)
class IOExecutorTest : BaseTestCase() {

    private lateinit var mExecutorService: ExecutorService
    private lateinit var executor: IOExecutor

    override fun setUp() {
        super.setUp()
        mExecutorService = mockk(relaxed = true)
        executor = IOExecutor()
        executor.setExecutorService(mExecutorService)
    }

    @Test
    fun test_executorServiceConfigs() {
        val executor = IOExecutor()
        Assert.assertTrue(executor.executorService is ThreadPoolExecutor)
        val executorService = executor.executorService as ThreadPoolExecutor
        val noProcessors = Runtime.getRuntime().availableProcessors()
        Assert.assertEquals(executorService.corePoolSize, 2 * noProcessors)
        Assert.assertEquals(executorService.maximumPoolSize, 2 * noProcessors)
        Assert.assertEquals(executorService.getKeepAliveTime(SECONDS), 60L)
    }

    @Test
    fun test_awaitTermination() {
        val timeout = 10L
        val unit = TimeUnit.MINUTES
        executor.awaitTermination(timeout, unit)
        verify { mExecutorService.awaitTermination(timeout, unit) }
    }

    @Test
    fun test_execute() {
        val runnable = Runnable { }
        executor.execute(runnable)
        verify { mExecutorService.execute(runnable) }
    }

    @Test
    fun test_invokeAll() {
        val taskCollection = ArrayList<Callable<Void>>()
        executor.invokeAll(taskCollection)
        verify { mExecutorService.invokeAll(taskCollection) }
    }

    @Test
    fun test_invokeAllWithTimeOut() {
        val taskCollection = ArrayList<Callable<Void>>()
        val timeout = 10L
        val unit = TimeUnit.MINUTES
        executor.invokeAll(taskCollection, timeout, unit)
        verify { mExecutorService.invokeAll(taskCollection, timeout, unit) }
    }

    @Test
    fun test_invokeAny() {
        val taskCollection = ArrayList<Callable<Void>>()
        executor.invokeAny(taskCollection)
        verify { mExecutorService.invokeAny(taskCollection) }
    }

    @Test
    fun test_invokeAnyWithTimeOut() {
        val taskCollection = ArrayList<Callable<Void>>()
        val timeout = 10L
        val unit = TimeUnit.MINUTES
        executor.invokeAny(taskCollection, timeout, unit)
        verify { mExecutorService.invokeAny(taskCollection, timeout, unit) }
    }

    @Test
    fun test_isShutdown() {
        executor.isShutdown
        verify { mExecutorService.isShutdown }
    }

    @Test
    fun test_isTerminated() {
        executor.isTerminated
        verify { mExecutorService.isTerminated }
    }

    @Test
    fun test_shutdown() {
        executor.shutdown()
        verify { mExecutorService.shutdown() }
    }

    @Test
    fun test_shutdownNow() {
        executor.shutdownNow()
        verify { mExecutorService.shutdownNow() }
    }

    @Test
    fun test_submit_Callable() {
        val callable = Callable { }
        executor.submit(callable)
        verify { mExecutorService.submit(callable) }
    }

    @Test
    fun test_submit_RunnableWithResult() {
        val callable = Runnable { }
        val result = "121"
        executor.submit(callable, result)
        verify { mExecutorService.submit(callable, result) }
    }

    @Test
    fun test_submit_RunnableOnly() {
        val callable = Runnable { }
        executor.submit(callable)
        verify { mExecutorService.submit(callable) }
    }
}
