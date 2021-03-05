package com.clevertap.android.sdk.pushnotification.amp

import android.content.Intent
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.*
import org.mockito.Mockito.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class CTBackgroundIntentServiceTest : BaseTestCase() {

    override fun setUp() {
        super.setUp()
    }

    @Test
    fun test_handleIntent() {
        mockStatic(CleverTapAPI::class.java).use {
            val exceptionMessage = "CleverTapAPI#runBackgroundIntentService called"
            `when`(CleverTapAPI.runBackgroundIntentService(any())).thenThrow(RuntimeException(exceptionMessage))

            val exception = assertThrows(RuntimeException::class.java) {
                val serviceController = Robolectric.buildIntentService(CTBackgroundIntentService::class.java, Intent())
                serviceController.create().handleIntent()
            }

            assertTrue(exceptionMessage.equals(exception.message))
        }
    }
}