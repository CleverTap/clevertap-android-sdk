package com.clevertap.android.xps

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.interfaces.INotificationParser
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.google.gson.GsonBuilder
import com.xiaomi.mipush.sdk.MiPushMessage
import org.junit.*
import org.junit.runner.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class XiaomiNotificationParserTest : BaseTestCase() {

    private lateinit var parser: INotificationParser<MiPushMessage>
    private lateinit var message: MiPushMessage

    @Before
    override fun setUp() {
        super.setUp()
        parser = XiaomiNotificationParser()
        message = mock(MiPushMessage::class.java)
    }

    @Ignore
    @Test
    fun testToBundle_Message_Invalid_Content_Return_Null() {
        `when`(message.content).thenReturn(null)
        Assert.assertNull(parser.toBundle(message))
    }

    @Ignore
    @Test
    fun testToBundle_Message_Outside_CleverTap_Return_Null() {
        `when`(message.content).thenReturn(getMockJsonStringOutsideNetwork())
        Assert.assertNull(parser.toBundle(message))
    }

    @Test
    fun testToBundle_Message_CleverTap_Message_Return_Not_Null() {
        `when`(message.content).thenReturn(getMockJsonStringClevertapNetwork())
        Assert.assertNotNull(parser.toBundle(message))
    }

    private fun getMockJsonStringOutsideNetwork(): String? {
        val hashMap = HashMap<String, String>()
        hashMap.put("Title", "Sample Title")
        hashMap.put("Message", "Sample Message Title")
        return GsonBuilder().create().toJson(hashMap)
    }

    private fun getMockJsonStringClevertapNetwork(): String? {
        val hashMap = HashMap<String, String>()
        hashMap.put("Title", "Sample Title")
        hashMap.put("Message", "Sample Message Title")
        hashMap.put(Constants.NOTIFICATION_TAG, "some value")
        return GsonBuilder().create().toJson(hashMap)
    }
}