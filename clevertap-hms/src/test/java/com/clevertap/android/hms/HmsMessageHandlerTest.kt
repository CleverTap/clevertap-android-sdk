package com.clevertap.android.hms

import android.content.Context
import android.os.Bundle
import com.clevertap.android.hms.HmsTestConstants.Companion.HMS_TOKEN
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.pushnotification.NotificationInfo
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
    @Ignore
    fun testCreateNotification_Invalid_Message() {
        val isSuccess = handler.createNotification(application, RemoteMessage(Bundle()))
        Assert.assertFalse(isSuccess)
    }

    @Test
    @Ignore
    fun testCreateNotification_Outside_CleverTap_Message() {
        val bundle = getMockBundle()
        val info = NotificationInfo(false, true)
        mockStatic(CleverTapAPI::class.java).use {
            `when`(CleverTapAPI.getNotificationInfo(any(Bundle::class.java))).thenReturn(info)
            mockStatic(Utils::class.java).use {
                `when`(Utils.stringToBundle(anyString())).thenReturn(bundle)
                val isSuccess = handler.createNotification(application, RemoteMessage(bundle))
                Assert.assertFalse(isSuccess)
            }
        }
    }

    @Test
    @Ignore
    fun testCreateNotification_Valid_Message() {
        val bundle = mock(Bundle::class.java)
        mockStatic(Utils::class.java).use {
            `when`(Utils.stringToBundle(anyString())).thenReturn(bundle)
            val isSuccess = handler.createNotification(application, RemoteMessage(Bundle()))
            Assert.assertTrue(isSuccess)
        }
    }

    private fun getMockBundle(): Bundle? {
        val bundle = Bundle()
        bundle.putString(Constants.NOTIFICATION_TAG, "some value")
        bundle.putString(Constants.WZRK_ACCT_ID_KEY, "some value")
        return bundle
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