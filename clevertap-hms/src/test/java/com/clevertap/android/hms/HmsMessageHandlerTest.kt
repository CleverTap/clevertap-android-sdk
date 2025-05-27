package com.clevertap.android.hms

import android.os.Bundle
import com.clevertap.android.hms.HmsConstants.HPS
import com.clevertap.android.hms.HmsTestConstants.Companion.HMS_TOKEN
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.interfaces.INotificationParser
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.huawei.hms.push.RemoteMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class HmsMessageHandlerTest : BaseTestCase() {

    private lateinit var mHandlerCT: CTHmsMessageHandler
    private lateinit var parser: INotificationParser<RemoteMessage>

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        parser = mockk<HmsNotificationParser>(relaxed = true)
        mHandlerCT = CTHmsMessageHandler(parser)
    }

    @Test
    fun testCreateNotification_Null_Message() {
        val isSuccess = mHandlerCT.createNotification(application, null)
        Assert.assertFalse(isSuccess)
    }

    @Test
    fun testCreateNotification_Invalid_Message_Throws_Exception() {
        val bundle = Bundle()
        every { parser.toBundle(any()) } returns bundle
        mockkStatic(CleverTapAPI::class) {
            every { CleverTapAPI.createNotification(application, bundle) } throws
                RuntimeException("Something went wrong")

            val isSuccess = mHandlerCT.createNotification(application, RemoteMessage(bundle))
            Assert.assertFalse(isSuccess)
        }
    }

    @Test
    fun testCreateNotification_Valid_Message() {
        val bundle = Bundle()
        bundle.putString(Constants.NOTIFICATION_TAG,"tag")
        bundle.putString(Constants.NOTIF_MSG,"msg")
        every { parser.toBundle(any()) } returns bundle
        val isSuccess = mHandlerCT.createNotification(application, RemoteMessage(bundle))
        Assert.assertTrue(isSuccess)
    }

    @Test
    fun testCreateNotification_Valid_Message_With_Account_ID() {
        val bundle = Bundle()
        bundle.putString(Constants.WZRK_ACCT_ID_KEY, "Some Value")
        bundle.putString(Constants.NOTIFICATION_TAG,"tag")
        bundle.putString(Constants.NOTIF_MSG,"msg")
        every { parser.toBundle(any()) } returns bundle
        val isSuccess = mHandlerCT.createNotification(application, RemoteMessage(Bundle()))
        Assert.assertTrue(isSuccess)
    }

    @Test
    fun testOnNewToken_Success() {
        Assert.assertTrue(mHandlerCT.onNewToken(application, HMS_TOKEN))
    }

    @Test
    fun testOnNewToken_Failure() {
        mockkStatic(CleverTapAPI::class) {
            every {
                CleverTapAPI.tokenRefresh(any(), eq(HMS_TOKEN), eq(HPS))
            } throws RuntimeException("Something Went Wrong")
            Assert.assertFalse(mHandlerCT.onNewToken(application, HMS_TOKEN))
        }
    }
}