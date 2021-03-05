package com.clevertap.android.sdk.pushnotification.amp

import android.app.job.JobParameters
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import org.junit.*
import org.junit.runner.*
import org.mockito.Mockito.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class CTBackgroundJobServiceTest : BaseTestCase() {

    private lateinit var service: CTBackgroundJobService
    private lateinit var mockParams: JobParameters
    override fun setUp() {
        super.setUp()
        service = spy(Robolectric.setupService(CTBackgroundJobService::class.java))
        mockParams = mock(JobParameters::class.java)
    }

    @Test
    fun test_onStartJob() {

    }
}