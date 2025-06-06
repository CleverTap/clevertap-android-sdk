package com.clevertap.android.sdk.task

import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class PostAsyncSafelyExecutorTest : BaseTestCase() {

    private lateinit var mExecutorService: ExecutorService
    private lateinit var executor: PostAsyncSafelyExecutor

    override fun setUp() {
        super.setUp()
        mExecutorService = mockk(relaxed = true)
        executor = PostAsyncSafelyExecutor()
        executor.setExecutor(mExecutorService)
    }

    @Test
    fun test_executorServiceConfigs() {
        val executor = PostAsyncSafelyExecutor()
        Assert.assertTrue(executor.executor is ExecutorService)
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
        executor.execute({
            println("Do something")
        })
        verify { mExecutorService.execute(any()) }
    }

    @Test
    @Ignore
    fun test_execute_whenExecuteCalledFromAsyncThread() {
        //TODO
        var threadID1 = -1L
        var threadID2 = -2L
        executor.execute({
            threadID1 = Thread.currentThread().id
            executor.execute({
                threadID2 = Thread.currentThread().id
            })
        })
        verify { mExecutorService.execute(any()) }
        Thread.sleep(1000)
        Assert.assertEquals(threadID1, threadID2)
    }

    @Test(expected = NullPointerException::class)
    fun test_execute_WhenNullRunnable_ThrowsNPE() {
        executor.execute(null)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun test_invokeAll() {
        val taskCollection = ArrayList<Callable<Void>>()
        executor.invokeAll(taskCollection)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun test_invokeAllWithTimeOut() {
        val taskCollection = ArrayList<Callable<Void>>()
        val timeout = 10L
        val unit = TimeUnit.MINUTES
        executor.invokeAll(taskCollection, timeout, unit)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun test_invokeAny() {
        val taskCollection = ArrayList<Callable<Void>>()
        executor.invokeAny(taskCollection)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun test_invokeAnyWithTimeOut() {
        val taskCollection = ArrayList<Callable<Void>>()
        val timeout = 10L
        val unit = TimeUnit.MINUTES
        executor.invokeAny(taskCollection, timeout, unit)
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

    @Test(expected = NullPointerException::class)
    fun test_submit_WhenNullCallable_ThrowsNPE() {
        executor.submit(null)
    }

    @Test
    fun test_submit_Callable() {
        val callable = Callable { }
        executor.submit(callable)
        verify { mExecutorService.submit(any<Callable<Any>>()) }
    }

    @Test
    fun test_submit_RunnableWithResult() {
        val callable = Runnable { }
        val result = "121"
        executor.submit(callable, result)
        verify { mExecutorService.execute(any<Runnable>()) }
    }

    @Test(expected = NullPointerException::class)
    fun test_submitWithResult_WhenNullRunnable_ThrowNPE() {
        val result = "121"
        executor.submit(null, result)
    }

    @Test
    fun test_submit_RunnableOnly() {
        val callable = Runnable { }
        executor.submit(callable)
        verify { mExecutorService.execute(any()) }
    }

    @Test(expected = NullPointerException::class)
    fun test_submitRunnableOnly_WhenNullRunnable_ThrowsNPE() {
        executor.submit(null)
    }
}
