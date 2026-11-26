package com.clevertap.android.sdk.task

import android.os.Handler
import android.os.Looper
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class MainThreadExecutorTest : BaseTestCase() {

    @Test
    fun test_execute() {
        val handler = mockk<Handler>(relaxed = true)
        val executor = MainThreadExecutor()
        executor.mainThreadHandler = handler
        val runnable = Runnable { }
        executor.execute(runnable)
        verify { handler.post(runnable) }
    }

    @Test
    fun test_setMainThreadHandler() {
        val handler = Handler()
        val executor = MainThreadExecutor()
        executor.mainThreadHandler = handler
        assertEquals(executor.mainThreadHandler, handler)
    }

    @Test
    fun test_execute_whenRunnableRuns_RunsOnMainThread() {
        val executor = MainThreadExecutor()
        val runnable = Runnable {
            assertTrue(Looper.myLooper() == Looper.getMainLooper())
        }
        executor.execute(runnable)

        //or
        assertTrue(executor.mainThreadHandler.looper == Looper.getMainLooper())
    }
}
