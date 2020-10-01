package com.clevertap.android.xps

import android.content.Context
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.xiaomi.mipush.sdk.MiPushCommandMessage
import com.xiaomi.mipush.sdk.MiPushMessage
import org.junit.*
import org.junit.runner.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class XiaomiMessageReceiverTest : BaseTestCase() {

    private lateinit var receiver: XiaomiMessageReceiver
    private lateinit var handler: XiaomiMessageHandlerImpl

    @Before
    override fun setUp() {
        super.setUp()
        receiver = XiaomiMessageReceiver()
        handler = mock(XiaomiMessageHandlerImpl::class.java)
        receiver.setHandler(handler)
    }

    @Test
    fun testOnReceivePassThroughMessage() {
        receiver.onReceivePassThroughMessage(application, MiPushMessage())
        verify(handler).createNotification(any(Context::class.java), any(MiPushMessage::class.java))
    }

    @Test
    fun testOnReceiveRegisterResult() {
        receiver.onReceiveRegisterResult(application, MiPushCommandMessage())
        verify(handler)
            .onReceiveRegisterResult(any(Context::class.java), any(MiPushCommandMessage::class.java))
    }

    @Test
    fun testOnNotificationMessageArrived() {
        receiver.onNotificationMessageArrived(application, MiPushMessage())
        verify(handler).createNotification(any(Context::class.java), any(MiPushMessage::class.java))
    }
}