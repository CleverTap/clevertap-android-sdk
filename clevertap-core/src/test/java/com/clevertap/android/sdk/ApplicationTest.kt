package com.clevertap.android.sdk

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.runner.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApplicationTest : BaseTestCase() {

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun test_getCoreState_ReturnedObjectMustHaveNonNullInAppFCManagerInsideControllerManager() {
        val application = Application()
        mockStatic(ActivityLifecycleCallback::class.java).use {
            application.onCreate()
            it.verify { ActivityLifecycleCallback.register(application) }
        }
    }
}