package com.clevertap.android.sdk.inapp

import android.content.Context
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InAppActionHandlerTest : BaseTestCase() {

    private val inAppActionHandler = InAppActionHandler(mockk<Logger>(relaxed = true))

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
}
