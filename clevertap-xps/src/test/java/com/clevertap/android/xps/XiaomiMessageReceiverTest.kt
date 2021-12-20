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
    private lateinit var mHandlerCT: CTXiaomiMessageHandler

    @Before
    override fun setUp() {
        super.setUp()
        receiver = XiaomiMessageReceiver()
        mHandlerCT = mock(CTXiaomiMessageHandler::class.java)
        receiver.setHandler(mHandlerCT)
    }

    @Test
    fun testOnReceivePassThroughMessage() {
        receiver.onReceivePassThroughMessage(application, MiPushMessage())
        verify(mHandlerCT).createNotification(any(Context::class.java), any(MiPushMessage::class.java))
    }

    @Test
    fun testOnReceiveRegisterResult() {
        receiver.onReceiveRegisterResult(application, MiPushCommandMessage())
        verify(mHandlerCT)
            .onReceiveRegisterResult(any(Context::class.java), any(MiPushCommandMessage::class.java))
    }

    @Test
    fun testOnNotificationMessageArrived() {
        receiver.onNotificationMessageArrived(application, MiPushMessage())
        verify(mHandlerCT).createNotification(any(Context::class.java), any(MiPushMessage::class.java))
    }
}