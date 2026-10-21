package com.clevertap.android.pushtemplates

import com.clevertap.android.sdk.CleverTapAPI
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PushTemplateNotificationHandlerTest {

    private lateinit var handler: PushTemplateNotificationHandler

    @Before
    fun setUp() {
        handler = PushTemplateNotificationHandler()
        CleverTapAPI.setInstances(null)
        CleverTapAPI.setNotificationHandler(null)
    }

    @After
    fun tearDown() {
        CleverTapAPI.setNotificationHandler(null)
        CleverTapAPI.setInstances(null)
    }

    @Test
    fun test_getCustomSdkNameAndVersion_returnPushTemplatesValues() {
        assertEquals(PTConstants.PT_SDK_NAME, handler.customSdkName)
        assertEquals(BuildConfig.VERSION_CODE, handler.customSdkVersion)
    }

    @Test
    fun test_setNotificationHandler_registersPushTemplatesVersionOnExistingInstance() {
        val instance = mockk<CleverTapAPI>(relaxed = true)
        CleverTapAPI.setInstances(hashMapOf("test-account-id" to instance))

        CleverTapAPI.setNotificationHandler(handler)

        verify(exactly = 1) { instance.setCustomSdkVersion(PTConstants.PT_SDK_NAME, BuildConfig.VERSION_CODE) }
    }
}
