package com.clevertap.android.hms

import android.content.Context
import android.os.Bundle
import com.clevertap.android.hms.HmsTestConstants.Companion.HMS_TOKEN
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType.HPS
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.huawei.hms.push.RemoteMessage
import org.junit.*
import org.junit.runner.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class HmsMessageHandlerTest : BaseTestCase() {

    private lateinit var handler: HmsMessageHandlerImpl
    private lateinit var parser: IHmsNotificationParser

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        parser = mock(HmsNotificationParser::class.java)
        handler = HmsMessageHandlerImpl(parser)
    }

    @Test
    fun testCreateNotification_Null_Message() {
        val isSuccess = handler.createNotification(application, null)
        Assert.assertFalse(isSuccess)
    }

    @Test
    fun testCreateNotification_Invalid_Message_Throws_Exception() {
        val bundle = Bundle()
        val isSuccess = handler.createNotification(application, RemoteMessage(bundle))
        `when`(parser.toBundle(any(RemoteMessage::class.java))).thenReturn(bundle)
        mockStatic(CleverTapAPI::class.java).use {
            `when`(CleverTapAPI.createNotification(application, bundle)).thenThrow(
                RuntimeException("Something went wrong")
            )
            Assert.assertFalse(isSuccess)
        }
    }

    @Test
    fun testCreateNotification_Valid_Message() {
        `when`(parser.toBundle(any(RemoteMessage::class.java))).thenReturn(Bundle())
        val isSuccess = handler.createNotification(application, RemoteMessage(Bundle()))
        Assert.assertTrue(isSuccess)
    }

    @Test
    fun testOnNewToken_Success() {
        Assert.assertTrue(handler.onNewToken(application, HMS_TOKEN))
    }

    @Test
    fun testOnNewToken_Failure() {
        mockStatic(CleverTapAPI::class.java).use {
            `when`(CleverTapAPI.tokenRefresh(any(Context::class.java), eq(HMS_TOKEN), eq(HPS)))
                .thenThrow(RuntimeException("Something Went Wrong"))
            Assert.assertFalse(handler.onNewToken(application, HMS_TOKEN))
        }
    }
}