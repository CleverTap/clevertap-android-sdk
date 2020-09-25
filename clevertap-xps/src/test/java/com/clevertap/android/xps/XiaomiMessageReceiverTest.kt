package com.clevertap.android.xps

import android.content.Context
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.xiaomi.mipush.sdk.MiPushCommandMessage
import com.xiaomi.mipush.sdk.MiPushMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class XiaomiMessageReceiverTest : BaseTestCase() {
    private lateinit var receiver: XiaomiMessageReceiver
    private lateinit var handler: XiaomiMessageHandler

    @Before
    override fun setUp() {
        super.setUp()
        receiver = XiaomiMessageReceiver()
        handler = Mockito.mock(XiaomiMessageHandler::class.java)
        receiver.setHandler(handler)
    }

    @Test
    fun testOnReceivePassThroughMessage() {
        receiver.onReceivePassThroughMessage(application, MiPushMessage())
        Mockito.verify(handler).createNotification(any(Context::class.java), any(MiPushMessage::class.java))
    }

    @Test
    fun testOnReceiveRegisterResult() {
        receiver.onReceiveRegisterResult(application, MiPushCommandMessage())
        Mockito.verify(handler).onReceiveRegisterResult(any(Context::class.java), any(MiPushCommandMessage::class.java))
    }

    @Test
    fun testOnNotificationMessageArrived() {
        receiver.onNotificationMessageArrived(application, MiPushMessage())
        Mockito.verify(handler).createNotification(any(Context::class.java), any(MiPushMessage::class.java))
    }
}