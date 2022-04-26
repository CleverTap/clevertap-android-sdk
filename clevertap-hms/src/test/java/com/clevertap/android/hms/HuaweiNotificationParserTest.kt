package com.clevertap.android.hms

import com.clevertap.android.sdk.Constants
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.google.gson.GsonBuilder
import com.huawei.hms.push.RemoteMessage
import org.junit.*
import org.junit.runner.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class HuaweiNotificationParserTest : BaseTestCase() {

    private lateinit var parser: HmsNotificationParser
    private lateinit var message: RemoteMessage

    @Before
    override fun setUp() {
        super.setUp()
        parser = HmsNotificationParser()
        message = mock(RemoteMessage::class.java)
    }

    @Test
    fun testToBundle_Message_Invalid_Content_Return_EmptyBundle() {
        `when`(message.data).thenReturn(null)
        val returnedBundle = parser.toBundle(message)
        Assert.assertNotNull(returnedBundle)
        Assert.assertEquals(0,returnedBundle.keySet().size)
    }

    @Test
    fun testToBundle_Message_Outside_CleverTap_Return_AssocBundle() {
        val mockJson= getMockJsonStringOutsideNetwork()
        `when`(message.data).thenReturn(mockJson)

        val returnedBundle = parser.toBundle(message)
        Assert.assertNotNull(returnedBundle)
        Assert.assertEquals(2,returnedBundle.keySet().size)
        Assert.assertEquals("Sample Title",returnedBundle.getString("Title"))
        Assert.assertEquals("Sample Message Title",returnedBundle.getString("Message"))

    }

    @Test
    fun testToBundle_Message_CleverTap_Message_Return_Not_Null() {
        `when`(message.data).thenReturn(getMockJsonStringClevertapNetwork())
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