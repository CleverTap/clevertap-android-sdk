package com.clevertap.android.sdk.task

import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.spyk
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class CTExecutorFactoryTest : BaseTestCase() {

    @Test(expected = IllegalArgumentException::class)
    fun test_executors_whenConfigNull_ShouldThrowException() {
        CTExecutorFactory.executors(null)
    }

    @Test
    fun test_executors_whenConfigNotNull_ShouldReturnValidObject() {
        val executor = CTExecutorFactory.executors(cleverTapInstanceConfig)
        assertTrue(executor is CTExecutors)
    }

    @Test
    fun test_executors_whenTwoDifferentConfigs_ShouldReturnDifferentObjects() {
        val executor = CTExecutorFactory.executors(cleverTapInstanceConfig)
        val newConfig = spyk(cleverTapInstanceConfig)
        every { newConfig.accountId } returns "312312312312"
        val executorNew = CTExecutorFactory.executors(newConfig)
        assertNotEquals(executor, executorNew)
    }
}
