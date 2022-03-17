package com.clevertap.android.sdk

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActivityLifecycleCallbackTest : BaseTestCase() {

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun test_register_whenApplicationPassedAsNullAndRegisteredIsTrue(){
        val applicationMock = mock(Application::class.java)
        ActivityLifecycleCallback.registered = true
        ActivityLifecycleCallback.register(null)
        verifyNoInteractions(applicationMock)
    }

    @Test
    fun test_register_whenRegisteredIsTrueAndApplicationIsNotNull(){
        val applicationMock = mock(Application::class.java)
        ActivityLifecycleCallback.registered = true
        ActivityLifecycleCallback.register(applicationMock)
        verifyNoInteractions(applicationMock)
    }

    @Test
    fun test_register_whenRegisteredIsTrueAndApplicationIsNull(){
        val applicationMock = mock(Application::class.java)
        ActivityLifecycleCallback.registered = true
        ActivityLifecycleCallback.register(null)
        verifyNoInteractions(applicationMock)
    }

    @Test
    fun test_register_whenRegisteredIsFalseAndApplicationIsNonNull_ActivityLifecycleCallbacksGetsRegisteredOnApplication(){
        val applicationMock = mock(Application::class.java)
        val captor = ArgumentCaptor.forClass(ActivityLifecycleCallbacks::class.java)
        ActivityLifecycleCallback.registered = false
        ActivityLifecycleCallback.register(applicationMock)
        verify(applicationMock).registerActivityLifecycleCallbacks(captor.capture())
    }

    @Test
    fun test_register_whenCleverTapIDIsNull_ActivityLifecycleCallbacksMethodsOfCleverTapAPIGetsCalled(){
        val applicationMock = mock(Application::class.java)
        val captor = ArgumentCaptor.forClass(ActivityLifecycleCallbacks::class.java)
        ActivityLifecycleCallback.registered = false
        ActivityLifecycleCallback.register(applicationMock)
        verify(applicationMock).registerActivityLifecycleCallbacks(captor.capture())
        val value : ActivityLifecycleCallbacks = captor.value
        mockStatic(CleverTapAPI::class.java).use {
            val mockActivity = mock(Activity::class.java)
            value.onActivityCreated(mockActivity, Bundle())
            it.verify { CleverTapAPI.onActivityCreated(mockActivity) }
            value.onActivityResumed(mockActivity)
            it.verify { CleverTapAPI.onActivityResumed(mockActivity) }
        }
    }

    @Test
    fun test_register_whenCleverTapIDIsNonNull_ActivityLifecycleCallbacksMethodsOfCleverTapAPIGetsCalledWithCleverTapID(){
        val applicationMock = mock(Application::class.java)
        val captor = ArgumentCaptor.forClass(ActivityLifecycleCallbacks::class.java)
        ActivityLifecycleCallback.registered = false
        ActivityLifecycleCallback.register(applicationMock,"1234567890")
        verify(applicationMock).registerActivityLifecycleCallbacks(captor.capture())
        val value : ActivityLifecycleCallbacks = captor.value
        mockStatic(CleverTapAPI::class.java).use {
            val mockActivity = mock(Activity::class.java)
            value.onActivityCreated(mockActivity, Bundle())
            it.verify { CleverTapAPI.onActivityCreated(mockActivity,"1234567890") }
            value.onActivityResumed(mockActivity)
            it.verify { CleverTapAPI.onActivityResumed(mockActivity,"1234567890") }
        }
    }

}