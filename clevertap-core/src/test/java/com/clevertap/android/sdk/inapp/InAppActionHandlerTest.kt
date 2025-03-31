package com.clevertap.android.sdk.inapp

import android.content.Context
import android.content.Intent
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.shared.test.BaseTestCase
import com.google.android.play.core.review.testing.FakeReviewManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InAppActionHandlerTest : BaseTestCase() {

    private lateinit var inAppActionHandler: InAppActionHandler

    @Before
    fun init() {
        inAppActionHandler = InAppActionHandler(
            application,
            getMockCtConfig(),
            pushPermissionHandler = mockk(relaxed = true),
            playStoreReviewManagerProvider = { FakeReviewManager(it) })
    }

    @Test
    fun `openUrl should return if an activity is found to handle the url`() {
        val url = "http://clevertap.com"
        val mockContext = mockk<Context>(relaxed = true)
        assertTrue(inAppActionHandler.openUrl(url, mockContext))

        every { mockContext.startActivity(any()) } throws Exception()
        assertFalse(inAppActionHandler.openUrl(url, mockContext))
    }

    @Test
    fun `openUrl should always return true when handling ct schema`() {
        val url = "${Constants.WZRK_URL_SCHEMA}clevertap.com"
        val mockContext = mockk<Context>(relaxed = true)
        assertTrue(inAppActionHandler.openUrl(url, mockContext))

        every { mockContext.startActivity(any()) } throws Exception()
        assertTrue(inAppActionHandler.openUrl(url, mockContext))
    }

    @Test
    fun `openUrl should include all url parameters as Intent extras`() {
        val param1Name = "param1"
        val param1Value = "value1"
        val param2Name = "param2"
        val param2Value = "value2"
        val url = "https://clevertap.com?$param1Name=$param1Value&$param2Name=$param2Value"
        val mockContext = mockk<Context>(relaxed = true)

        assertTrue(inAppActionHandler.openUrl(url, mockContext))

        verify {
            mockContext.startActivity(
                match { intent ->
                    intent.extras?.getString(param1Name) == param1Value
                            && intent.extras?.getString(param2Name) == param2Value
                })
        }
    }

    @Test
    fun `openUrl should add NEW_TASK Intent flag only when no launchContext is provided`() {
        val url = "https://clevertap.com"
        val mockContext = mockk<Context>(relaxed = true)
        val inAppActionHandler = InAppActionHandler(
            mockContext,
            getMockCtConfig(),
            pushPermissionHandler = mockk()
        )

        assertTrue(inAppActionHandler.openUrl(url))

        verify {
            mockContext.startActivity(
                match { intent ->
                    intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0
                })
        }

        val mockLaunchContext = mockk<Context>(relaxed = true)
        assertTrue(inAppActionHandler.openUrl(url, mockLaunchContext))

        verify {
            mockLaunchContext.startActivity(
                match { intent ->
                    intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK == 0
                })
        }
    }

    @Test
    fun `isPlayStoreReviewLibraryAvailable should return true when review library is included`() {
        assertTrue(inAppActionHandler.isPlayStoreReviewLibraryAvailable())
    }

    private fun getMockCtConfig(): CleverTapInstanceConfig {
        val mockCtConfig = mockk<CleverTapInstanceConfig>(relaxed = true)
        every { mockCtConfig.logger } returns mockk(relaxed = true)
        return mockCtConfig
    }

}
