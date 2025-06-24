package com.clevertap.android.sdk

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActivityLifecycleCallbackTest : BaseTestCase() {

    @Test
    fun test_register_whenApplicationPassedAsNullAndRegisteredIsTrue(){
        val applicationMock = mockk<Application>(relaxed = true)
        ActivityLifecycleCallback.registered = true
        ActivityLifecycleCallback.register(null)
        confirmVerified(applicationMock)
    }

    @Test
    fun test_register_whenRegisteredIsTrueAndApplicationIsNotNull(){
        val applicationMock = mockk<Application>(relaxed = true)
        ActivityLifecycleCallback.registered = true
        ActivityLifecycleCallback.register(applicationMock)
        confirmVerified(applicationMock)
    }

    @Test
    fun test_register_whenRegisteredIsTrueAndApplicationIsNull(){
        val applicationMock = mockk<Application>(relaxed = true)
        ActivityLifecycleCallback.registered = true
        ActivityLifecycleCallback.register(null)
        confirmVerified(applicationMock)
    }

    @Test
    fun test_register_whenRegisteredIsFalseAndApplicationIsNonNull_ActivityLifecycleCallbacksGetsRegisteredOnApplication(){
        val applicationMock = mockk<Application>(relaxed = true)
        ActivityLifecycleCallback.registered = false
        ActivityLifecycleCallback.register(applicationMock)
        verify { applicationMock.registerActivityLifecycleCallbacks(any()) }
    }

    @Test
    fun test_register_whenCleverTapIDIsNull_ActivityLifecycleCallbacksMethodsOfCleverTapAPIGetsCalled(){
        val applicationMock = mockk<Application>(relaxed = true)
        val callbackSlot = slot<ActivityLifecycleCallbacks>()
        ActivityLifecycleCallback.registered = false
        ActivityLifecycleCallback.register(applicationMock)
        verify { applicationMock.registerActivityLifecycleCallbacks(capture(callbackSlot)) }
        val value: ActivityLifecycleCallbacks = callbackSlot.captured
        mockkStatic(CleverTapAPI::class) {
            val mockActivity = mockk<Activity>(relaxed = true)
            value.onActivityCreated(mockActivity, Bundle())
            verify { CleverTapAPI.onActivityCreated(mockActivity, null) }
            value.onActivityResumed(mockActivity)
            verify { CleverTapAPI.onActivityResumed(mockActivity, null) }
        }
    }

    @Test
    fun test_register_whenCleverTapIDIsNonNull_ActivityLifecycleCallbacksMethodsOfCleverTapAPIGetsCalledWithCleverTapID(){
        val applicationMock = mockk<Application>(relaxed = true)
        val callbackSlot = slot<ActivityLifecycleCallbacks>()
        ActivityLifecycleCallback.registered = false
        ActivityLifecycleCallback.register(applicationMock,"1234567890")
        verify { applicationMock.registerActivityLifecycleCallbacks(capture(callbackSlot)) }
        val value: ActivityLifecycleCallbacks = callbackSlot.captured
        mockkStatic(CleverTapAPI::class) {
            val mockActivity = mockk<Activity>(relaxed = true)
            value.onActivityCreated(mockActivity, Bundle())
            verify { CleverTapAPI.onActivityCreated(mockActivity, "1234567890") }
            value.onActivityResumed(mockActivity)
            verify { CleverTapAPI.onActivityResumed(mockActivity, "1234567890") }
        }
    }

}