package com.clevertap.android.hms

import android.content.Context
import android.os.Bundle
import com.clevertap.android.hms.HmsTestConstants.Companion.HMS_TOKEN
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType.HPS
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.google.gson.GsonBuilder
import com.huawei.hms.push.RemoteMessage
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.mockito.ArgumentMatchers.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class HmsMessageHandlerTest : BaseTestCase() {

    private lateinit var handler: HmsMessageHandlerImpl

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        handler = HmsMessageHandlerImpl()
    }

    @Test
    fun testCreateNotification_Null_Message() {
        val isSuccess = handler.createNotification(application, null)
        Assert.assertFalse(isSuccess)
    }

    @Test
    fun testCreateNotification_Invalid_Message() {
        val isSuccess = handler.createNotification(application, RemoteMessage(Bundle()))
        Assert.assertFalse(isSuccess)
    }

    @Test
    fun testCreateNotification_Valid_Message() {
        val message = Mockito.mock(RemoteMessage::class.java)
        Mockito.`when`(message.data).thenReturn(getMockJsonString())
        val isSuccess = handler.createNotification(application, message)
        Assert.assertTrue(isSuccess)
    }

    private fun getMockJsonString(): String? {
        val hashMap = HashMap<String, String>()
        hashMap.put("Title", "Sample Title")
        hashMap.put("Message", "Sample Message Title")
        return GsonBuilder().create().toJson(hashMap)
    }

    @Test
    fun testOnNewToken_Success() {
        Assert.assertTrue(handler.onNewToken(application, HMS_TOKEN))
    }

    @Test
    fun testOnNewToken_Failure() {
        Mockito.mockStatic(CleverTapAPI::class.java).use {
            Mockito.`when`(CleverTapAPI.tokenRefresh(any(Context::class.java), eq(HMS_TOKEN), eq(HPS)))
                .thenThrow(RuntimeException("Something Went Wrong"))
            Assert.assertFalse(handler.onNewToken(application, HMS_TOKEN))
        }
    }
}