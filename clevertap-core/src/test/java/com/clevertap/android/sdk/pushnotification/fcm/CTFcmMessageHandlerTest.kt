package com.clevertap.android.sdk.pushnotification.fcm

import android.content.Context
import android.os.Bundle
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType.FCM
import com.clevertap.android.sdk.pushnotification.fcm.TestFcmConstants.Companion.FCM_TOKEN
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.google.firebase.messaging.RemoteMessage
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.mockito.ArgumentMatchers.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Ignore
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class CTFcmMessageHandlerTest : BaseTestCase() {

    private lateinit var mHandlerCT: CTFcmMessageHandler

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        mHandlerCT = CTFcmMessageHandler()
    }

    @Test
    fun testCreateNotification_Null_Message() {
        Assert.assertFalse(mHandlerCT.createNotification(application, null))
    }

    @Test
    fun testCreateNotification_Invalid_Message() {
        //empty bundle
        Assert.assertFalse(mHandlerCT.createNotification(application, RemoteMessage(Bundle())))
    }

    @Test
    fun testCreateNotification_Not_CleverTap_Message() {
        val bundle = Bundle()
        bundle.putString("title", "Test Title")
        bundle.putString("messagee", "Test Message")
        Assert.assertFalse(mHandlerCT.createNotification(application, RemoteMessage(bundle)))
    }

    @Test
    fun testCreateNotification_Valid_Message() {
        val bundle = Bundle()
        bundle.putString("title", "Test Title")
        bundle.putString("messagee", "Test Message")
        bundle.putString(Constants.NOTIFICATION_TAG, "Some Data")
        val isSuccess = mHandlerCT.createNotification(application, RemoteMessage(bundle))
        Assert.assertTrue(isSuccess)
    }

    @Test
    fun testOnNewToken_Success() {
        Assert.assertTrue(mHandlerCT.onNewToken(application, FCM_TOKEN))
    }

    @Test
    fun testOnNewToken_Failure() {
        Mockito.mockStatic(CleverTapAPI::class.java).use {
            Mockito.`when`(CleverTapAPI.tokenRefresh(any(Context::class.java), eq(FCM_TOKEN), FCM))
                .thenThrow(RuntimeException("Something Went Wrong"))
            Assert.assertFalse(mHandlerCT.onNewToken(application, FCM_TOKEN))
        }
    }
}