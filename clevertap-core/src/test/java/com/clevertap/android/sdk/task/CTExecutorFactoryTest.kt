package com.clevertap.android.sdk.task

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.robolectric.RobolectricTestRunner

//@RunWith(RobolectricTestRunner::class)
//class CTExecutorFactoryTest : BaseTestCase() {
//
//    override fun setUp() {
//        super.setUp()
//    }
//
//    @Test(expected = IllegalArgumentException::class)
//    fun test_executors_whenConfigNull_ShouldThrowException() {
//        CTExecutorFactory.executors(null)
//    }
//
//    @Test
//    fun test_executors_whenConfigNotNull_ShouldReturnValidObject() {
//        val executor = CTExecutorFactory.executors(cleverTapInstanceConfig)
//        Assert.assertTrue(executor is CTExecutors)
//    }
//
//    @Test
//    fun test_executors_whenTwoDiffrentConfigs_ShouldReturnDifferentObjects() {
//        val executor = CTExecutorFactory.executors(cleverTapInstanceConfig)
//        val newConfig = Mockito.spy(cleverTapInstanceConfig)
//        Mockito.`when`(newConfig.accountId).thenReturn("312312312312")
//        val executorNew = CTExecutorFactory.executors(newConfig)
//        Assert.assertNotEquals(executor, executorNew)
//    }
//}