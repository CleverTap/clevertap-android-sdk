package com.clevertap.android.hms

import com.clevertap.android.sdk.Constants
import com.clevertap.android.shared.test.BaseTestCase
import com.huawei.hms.push.RemoteMessage
import io.mockk.every
import io.mockk.mockk
import org.json.JSONObject
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HuaweiNotificationParserTest : BaseTestCase() {

    private lateinit var parser: HmsNotificationParser
    private lateinit var message: RemoteMessage

    @Before
    override fun setUp() {
        super.setUp()
        parser = HmsNotificationParser()
        message = mockk<RemoteMessage>(relaxed = true)
    }

    @Test
    fun testToBundle_Message_Invalid_Content_Return_EmptyBundle() {
        every { message.data } returns null
        val returnedBundle = parser.toBundle(message)
        Assert.assertNotNull(returnedBundle)
        Assert.assertEquals(0, returnedBundle.keySet().size)
    }

    @Test
    fun testToBundle_Message_Outside_CleverTap_Return_AssocBundle() {
        val mockJson = getMockJsonStringOutsideNetwork()
        every { message.data } returns mockJson

        val returnedBundle = parser.toBundle(message)
        Assert.assertNotNull(returnedBundle)
        Assert.assertEquals(2, returnedBundle.keySet().size)
        Assert.assertEquals("Sample Title", returnedBundle.getString("Title"))
        Assert.assertEquals("Sample Message Title", returnedBundle.getString("Message"))

    }

    @Test
    fun testToBundle_Message_CleverTap_Message_Return_Not_Null() {
        every { message.data } returns getMockJsonStringClevertapNetwork()
        Assert.assertNotNull(parser.toBundle(message))
    }

    private fun getMockJsonStringOutsideNetwork(): String? {
        val json = JSONObject()
        json.put("Title", "Sample Title")
        json.put("Message", "Sample Message Title")
        return json.toString()
    }

    private fun getMockJsonStringClevertapNetwork(): String? {
        val json = JSONObject()
        json.put("Title", "Sample Title")
        json.put("Message", "Sample Message Title")
        json.put(Constants.NOTIFICATION_TAG, "some value")
        return json.toString()
    }
}