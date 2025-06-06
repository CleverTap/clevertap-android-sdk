package com.clevertap.android.hms

import android.content.Context
import android.os.Bundle
import com.clevertap.android.hms.HmsTestConstants.Companion.HMS_TOKEN
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.huawei.hms.push.RemoteMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
@Ignore("Make IHmsMessageHandler be a provided dependency instead of internally created")
class HmsMessageServiceTest : BaseTestCase() {

    private lateinit var service: CTHmsMessageService
    private lateinit var mMockedMessageHandlerCT: CTHmsMessageHandler

    @Before
    override fun setUp() {
        super.setUp()
        service = spyk<CTHmsMessageService>(recordPrivateCalls = true)
        mMockedMessageHandlerCT = mockk<CTHmsMessageHandler>(relaxed = true)
        every { service.applicationContext } returns application
    }

    @Test
    fun testOnNewToken() {
        service.onNewToken(HMS_TOKEN)
        verify(exactly = 1) {
            mMockedMessageHandlerCT.onNewToken(any(), eq(HMS_TOKEN))
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
