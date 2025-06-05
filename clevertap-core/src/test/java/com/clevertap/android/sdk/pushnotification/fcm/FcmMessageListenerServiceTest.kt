package com.clevertap.android.sdk.pushnotification.fcm

import android.content.Intent
import android.os.Bundle
import com.clevertap.android.sdk.pushnotification.fcm.TestFcmConstants.Companion.FCM_TOKEN
import com.clevertap.android.shared.test.BaseTestCase
import com.google.firebase.messaging.RemoteMessage
import io.mockk.mockk
import io.mockk.verify
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@Ignore("Provide handler outside of FcmMessageListenerService so it can be tested")
@RunWith(RobolectricTestRunner::class)
class FcmMessageListenerServiceTest : BaseTestCase() {

    private lateinit var service: FcmMessageListenerService
    private lateinit var mMockedMessageHandlerCT: CTFcmMessageHandler

    override fun setUp() {
        super.setUp()
        val serviceController = Robolectric.buildService(FcmMessageListenerService::class.java, Intent())
        serviceController.create().startCommand(0, 1)
        service = serviceController.get()
        mMockedMessageHandlerCT = mockk(relaxed = true)
    }

    @Test
    fun testOnNewToken() {
        service.onNewToken(FCM_TOKEN)
        verify(exactly = 1) {
            mMockedMessageHandlerCT.onNewToken(any(), FCM_TOKEN)
        }
    }

    @Test
    fun testOnMessageReceived() {
        service.onMessageReceived(RemoteMessage(Bundle()))
        verify(exactly = 1) {
            mMockedMessageHandlerCT.createNotification(any(), any())
        }
    }
}
