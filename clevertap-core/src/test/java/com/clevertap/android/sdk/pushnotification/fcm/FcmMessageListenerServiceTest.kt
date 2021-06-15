package com.clevertap.android.sdk.pushnotification.fcm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.clevertap.android.sdk.pushnotification.fcm.TestFcmConstants.Companion.FCM_TOKEN
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.google.firebase.messaging.RemoteMessage
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Ignore
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class FcmMessageListenerServiceTest : BaseTestCase() {

    private lateinit var service: FcmMessageListenerService
    private lateinit var mockedMessageHandler: FcmMessageHandlerImpl

    @Before
    override fun setUp() {
        super.setUp()
        val serviceController = Robolectric.buildService(FcmMessageListenerService::class.java, Intent())
        serviceController.create().startCommand(0, 1)
        service = serviceController.get()
        mockedMessageHandler = Mockito.mock(FcmMessageHandlerImpl::class.java)
    }

    @Test
    fun testOnNewToken() {
        try {
            service.onNewToken(FCM_TOKEN)
            Mockito.verify(
                mockedMessageHandler.onNewToken(
                    Mockito.any(Context::class.java),
                    Mockito.eq(FCM_TOKEN)
                ), Mockito.times(1)
            )
        } catch (e: Exception) {

        }
    }

    @Test
    fun testOnMessageReceived() {
        try {
            service.onMessageReceived(RemoteMessage(Bundle()))
            Mockito.verify(
                mockedMessageHandler.onMessageReceived(
                    Mockito.any(Context::class.java),
                    Mockito.any(RemoteMessage::class.java)
                ), Mockito.times(1)
            )
        } catch (e: Exception) {

        }
    }
}