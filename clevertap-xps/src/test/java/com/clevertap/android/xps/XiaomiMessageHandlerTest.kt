package com.clevertap.android.xps

import android.os.Bundle
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.interfaces.INotificationParser
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.clevertap.android.xps.XpsConstants.FAILED_WITH_EXCEPTION
import com.clevertap.android.xps.XpsConstants.OTHER_COMMAND
import com.clevertap.android.xps.XpsConstants.TOKEN_SUCCESS
import com.clevertap.android.xps.XpsTestConstants.Companion.MI_TOKEN
import com.xiaomi.mipush.sdk.ErrorCode.ERROR_SERVICE_UNAVAILABLE
import com.xiaomi.mipush.sdk.ErrorCode.SUCCESS
import com.xiaomi.mipush.sdk.MiPushClient.COMMAND_REGISTER
import com.xiaomi.mipush.sdk.MiPushClient.COMMAND_UNSET_ACCOUNT
import com.xiaomi.mipush.sdk.MiPushCommandMessage
import com.xiaomi.mipush.sdk.MiPushMessage
import org.junit.*
import org.junit.runner.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class XiaomiMessageHandlerTest : BaseTestCase() {

    private lateinit var mHandlerCT: CTXiaomiMessageHandler
    private lateinit var parser: INotificationParser<MiPushMessage>

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        parser = mock(XiaomiNotificationParser::class.java)
        mHandlerCT = CTXiaomiMessageHandler(parser)
    }

    @Test
    fun testCreateNotification_Null_Message() {
        val isSuccess = mHandlerCT.createNotification(application, null)
        Assert.assertFalse(isSuccess)
    }

    @Test
    fun testCreateNotification_Invalid_Message_Throws_Exception() {
        val bundle = Bundle()
        `when`(parser.toBundle(any(MiPushMessage::class.java))).thenReturn(bundle)
        mockStatic(CleverTapAPI::class.java).use {
            `when`(CleverTapAPI.createNotification(application, bundle)).thenThrow(
                RuntimeException("Something went wrong")
            )
            val isSuccess = mHandlerCT.createNotification(application, MiPushMessage())
            Assert.assertFalse(isSuccess)
        }
    }

    @Ignore
    @Test
    fun testCreateNotification_Valid_Message() {
        `when`(parser.toBundle(any(MiPushMessage::class.java))).thenReturn(Bundle())
        val isSuccess = mHandlerCT.createNotification(application, MiPushMessage())
        Assert.assertTrue(isSuccess)
    }

    @Ignore
    @Test
    fun testCreateNotification_Valid_Message_With_Account_ID() {
        val bundle = Bundle()
        bundle.putString(Constants.WZRK_ACCT_ID_KEY, "Some Value")
        `when`(parser.toBundle(any(MiPushMessage::class.java))).thenReturn(bundle)
        val isSuccess = mHandlerCT.createNotification(application, MiPushMessage())
        Assert.assertTrue(isSuccess)
    }

    @Test
    fun testOnReceivePassThroughMessage_Other_Command() {
        val message = mock(MiPushCommandMessage::class.java)
        `when`(message.command).thenReturn(COMMAND_UNSET_ACCOUNT)
        val result = mHandlerCT.onReceiveRegisterResult(application, message)
        Assert.assertEquals(result, OTHER_COMMAND)
    }

    @Test
    fun testOnReceivePassThroughMessage_Token_Success() {
        val message = mock(MiPushCommandMessage::class.java)
        `when`(message.command).thenReturn(COMMAND_REGISTER)
        `when`(message.resultCode).thenReturn(SUCCESS.toLong())
        `when`(message.commandArguments).thenReturn(listOf(MI_TOKEN))
        val result = mHandlerCT.onReceiveRegisterResult(application, message)
        Assert.assertEquals(result, TOKEN_SUCCESS)
    }

    @Test
    fun testOnReceivePassThroughMessage_Invalid_Token() {
        val message = mock(MiPushCommandMessage::class.java)
        `when`(message.command).thenReturn(COMMAND_REGISTER)
        `when`(message.resultCode).thenReturn(SUCCESS.toLong())
        `when`(message.commandArguments).thenReturn(emptyList())
        val result = mHandlerCT.onReceiveRegisterResult(application, message)
        Assert.assertEquals(result, XpsConstants.INVALID_TOKEN)
    }

    @Test
    fun testOnReceivePassThroughMessage_Token_Failure() {
        val message = mock(MiPushCommandMessage::class.java)
        `when`(message.command).thenReturn(COMMAND_REGISTER)
        `when`(message.resultCode).thenReturn(ERROR_SERVICE_UNAVAILABLE.toLong())
        `when`(message.commandArguments).thenReturn(emptyList())
        val result = mHandlerCT.onReceiveRegisterResult(application, message)
        Assert.assertEquals(result, XpsConstants.TOKEN_FAILURE)
    }

    @Test
    fun testOnReceivePassThroughMessage_Failed_Exception() {
        val message = mock(MiPushCommandMessage::class.java)
        `when`(message.command).thenReturn(COMMAND_REGISTER)
        `when`(message.resultCode).thenReturn(SUCCESS.toLong())
        `when`(message.commandArguments).thenThrow(RuntimeException("Something went wrong"))
        val result = mHandlerCT.onReceiveRegisterResult(application, message)
        Assert.assertEquals(result, FAILED_WITH_EXCEPTION)
    }
}