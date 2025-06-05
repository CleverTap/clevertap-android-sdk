package com.clevertap.android.sdk.pushnotification.fcm

import android.os.Bundle
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.pushnotification.PushConstants.FCM
import com.clevertap.android.sdk.pushnotification.fcm.TestFcmConstants.Companion.FCM_TOKEN
import com.clevertap.android.shared.test.BaseTestCase
import com.google.firebase.messaging.RemoteMessage
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CTFcmMessageHandlerTest : BaseTestCase() {

    private lateinit var mHandlerCT: CTFcmMessageHandler

    @Before
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
        mockkStatic(CleverTapAPI::class) {
            every {
                CleverTapAPI.tokenRefresh(
                    any(),
                    FCM_TOKEN,
                    FCM
                )
            } throws RuntimeException("Something Went Wrong")
            Assert.assertFalse(mHandlerCT.onNewToken(application, FCM_TOKEN))
        }
    }
}
