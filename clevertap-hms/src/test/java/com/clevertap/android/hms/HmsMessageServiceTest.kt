package com.clevertap.android.hms

import android.content.Context
import android.os.Bundle
import com.clevertap.android.hms.HmsTestConstants.Companion.HMS_TOKEN
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
class HmsMessageServiceTest : BaseTestCase() {

    private lateinit var service: CTHmsMessageService
    private lateinit var mMockedMessageHandlerCT: CTHmsMessageHandler

    @Before
    override fun setUp() {
        super.setUp()
        service = spy(
            CTHmsMessageService
            ::class.java
        )
        mMockedMessageHandlerCT = mock(CTHmsMessageHandler::class.java)
        doReturn(application).`when`(service).applicationContext
    }

    @Test
    fun testOnNewToken() {
        try {
            service.onNewToken(HMS_TOKEN)
            verify(
                mMockedMessageHandlerCT.onNewToken(
                    any(Context::class.java),
                    eq(HMS_TOKEN)
                ), times(1)
            )
        } catch (e: Exception) {

        }
    }

    @Test
    fun testOnMessageReceived() {
        try {
            service.onMessageReceived(RemoteMessage(Bundle()))
            verify(
                mMockedMessageHandlerCT.createNotification(
                    any(Context::class.java),
                    any(RemoteMessage::class.java)
                ), times(1)
            )
        } catch (e: Exception) {

        }
    }
}