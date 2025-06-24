package com.clevertap.android.sdk

import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.*
import org.junit.runner.*
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
        mockkStatic(ActivityLifecycleCallback::class) {
            application.onCreate()
            verify { ActivityLifecycleCallback.register(application) }
        }
    }
}