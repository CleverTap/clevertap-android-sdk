package com.clevertap.android.sdk.task

import android.os.Handler
import android.os.Looper
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainThreadExecutorTest : BaseTestCase() {

    @Before
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun test_execute() {
        val handler = Mockito.mock(Handler::class.java)
        val executor = MainThreadExecutor()
        executor.mainThreadHandler = handler
        val runnable = Runnable { }
        executor.execute(runnable)
        Mockito.verify(handler).post(runnable)
    }

    @Test
    fun test_setMainThreadHandler() {
        val handler = Handler()
        val executor = MainThreadExecutor()
        executor.setMainThreadHandler(handler)
        Assert.assertEquals(executor.mainThreadHandler, handler)
    }

    @Test
    fun test_execute_whenRunnableRuns_RunsOnMainThread() {
        val executor = MainThreadExecutor()
        val runnable = Runnable {
            Assert.assertTrue(Looper.myLooper() == Looper.getMainLooper())
        }
        executor.execute(runnable)

        //or
        Assert.assertTrue(executor.mainThreadHandler.looper == Looper.getMainLooper())
    }
}