package com.clevertap.android.pushtemplates

import com.clevertap.android.sdk.CleverTapAPI
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PushTemplateNotificationHandlerTest {

    @Before
    fun setUp() {
        CleverTapAPI.setInstances(null)
    }

    @After
    fun tearDown() {
        CleverTapAPI.setInstances(null)
    }

    @Test
    fun test_constructor_whenInstancesExist_registersVersionOnEach() {
        val first = mockk<CleverTapAPI>(relaxed = true)
        val second = mockk<CleverTapAPI>(relaxed = true)
        CleverTapAPI.setInstances(hashMapOf("acc-1" to first, "acc-2" to second))

        PushTemplateNotificationHandler()

        verify(exactly = 1) { first.setCustomSdkVersion(PTConstants.PT_SDK_NAME, BuildConfig.VERSION_CODE) }
        verify(exactly = 1) { second.setCustomSdkVersion(PTConstants.PT_SDK_NAME, BuildConfig.VERSION_CODE) }
    }

    @Test
    fun test_constructor_whenNoInstancesYet_doesNotCrash() {
        PushTemplateNotificationHandler()
    }

    @Test
    fun test_registerSdkVersion_registersPushTemplatesValues() {
        val instance = mockk<CleverTapAPI>(relaxed = true)

        PushTemplateNotificationHandler.registerSdkVersion(instance)

        verify(exactly = 1) { instance.setCustomSdkVersion(PTConstants.PT_SDK_NAME, BuildConfig.VERSION_CODE) }
    }

    @Test
    fun test_registerSdkVersion_whenNullInstance_doesNotCrash() {
        PushTemplateNotificationHandler.registerSdkVersion(null)
    }

    @Test
    fun test_registerSdkVersion_whenSetCustomSdkVersionThrows_doesNotCrash() {
        val instance = mockk<CleverTapAPI>(relaxed = true)
        every { instance.setCustomSdkVersion(any(), any()) } throws NoSuchMethodError("setCustomSdkVersion")

        PushTemplateNotificationHandler.registerSdkVersion(instance)

        verify(exactly = 1) { instance.setCustomSdkVersion(PTConstants.PT_SDK_NAME, BuildConfig.VERSION_CODE) }
    }
}
