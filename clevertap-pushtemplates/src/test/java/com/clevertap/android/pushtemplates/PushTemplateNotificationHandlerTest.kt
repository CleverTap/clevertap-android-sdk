package com.clevertap.android.pushtemplates

import com.clevertap.android.sdk.CleverTapAPI
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PushTemplateNotificationHandlerTest {

    @Test
    fun test_constructor_registersSdkVersionWithCore() {
        mockkStatic(CleverTapAPI::class) {
            PushTemplateNotificationHandler()

            verify(exactly = 1) {
                CleverTapAPI.registerCustomSdkVersion(PTConstants.PT_SDK_NAME, BuildConfig.VERSION_CODE)
            }
        }
    }

    @Test
    fun test_constructor_whenCoreHasNoRegisterCustomSdkVersion_doesNotCrash() {
        mockkStatic(CleverTapAPI::class) {
            every { CleverTapAPI.registerCustomSdkVersion(any(), any()) } throws NoSuchMethodError("registerCustomSdkVersion")

            PushTemplateNotificationHandler()
        }
    }
}
