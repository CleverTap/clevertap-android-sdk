package com.clevertap.android.xps

import com.clevertap.android.sdk.ManifestInfo
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.xiaomi.mipush.sdk.MiPushClient
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class XiaomiSdkHandlerTest : BaseTestCase() {

    private lateinit var handler: XiaomiSdkHandler
    private lateinit var manifestInfo: ManifestInfo

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        manifestInfo = Mockito.mock(ManifestInfo::class.java)
        handler = XiaomiSdkHandler(application, cleverTapInstanceConfig)
        handler.setManifestInfo(manifestInfo)
    }

    @Test
    fun testOnNewToken_Without_Registration() {
        Mockito.mockStatic(MiPushClient::class.java).use {
            Mockito.`when`(MiPushClient.getRegId(application)).thenReturn(null)
            val token = handler.onNewToken()
            Assert.assertNull(token)
        }
    }

    @Test
    fun testOnNewToken_After_Registration() {
        Mockito.`when`(manifestInfo.xiaomiAppKey).thenReturn(XpsTestConstants.MI_APP_KEY)
        Mockito.`when`(manifestInfo.xiaomiAppID).thenReturn(XpsTestConstants.MI_APP_ID)
        handler.register(manifestInfo.xiaomiAppID, manifestInfo.xiaomiAppKey)
        Mockito.mockStatic(MiPushClient::class.java).use {
            Mockito.`when`(MiPushClient.getRegId(application)).thenReturn("abc")
            val token = handler.onNewToken()
            Assert.assertNotNull(token)
        }
    }

    @Test
    fun testOnNewTokenException() {
        Mockito.mockStatic(MiPushClient::class.java).use {
            Mockito.`when`(MiPushClient.getRegId(application)).thenThrow(RuntimeException("Checking"))
            val token = handler.onNewToken()
            Assert.assertNull(token)
        }
    }

    @Test
    fun testAppKey() {
        Mockito.`when`(manifestInfo.xiaomiAppKey).thenReturn(XpsTestConstants.MI_APP_KEY)
        Assert.assertEquals(handler.appKey(), XpsTestConstants.MI_APP_KEY)
        Mockito.`when`(manifestInfo.xiaomiAppKey).thenReturn(null)
        Assert.assertNull(handler.appKey())
    }

    @Test
    fun testAppId() {
        Mockito.`when`(manifestInfo.xiaomiAppID).thenReturn("abc")
        Assert.assertNotNull(handler.appId())
        Mockito.`when`(manifestInfo.xiaomiAppID).thenReturn(null)
        Assert.assertNull(handler.appId())
    }

    @Test
    fun testIsAvailable_validAppId_validAppKey() {
        Mockito.`when`(manifestInfo.xiaomiAppID).thenReturn("abc")
        Mockito.`when`(manifestInfo.xiaomiAppKey).thenReturn("xyz")
        Assert.assertTrue(handler.isAvailable)
    }

    @Test
    fun testIsAvailable_validAppId_inValidAppKey() {
        Mockito.`when`(manifestInfo.xiaomiAppID).thenReturn("abc")
        Mockito.`when`(manifestInfo.xiaomiAppKey).thenReturn("")
        Assert.assertFalse(handler.isAvailable)
    }

    @Test
    fun testIsAvailable_inValidAppId_ValidAppKey() {
        Mockito.`when`(manifestInfo.xiaomiAppID).thenReturn(null)
        Mockito.`when`(manifestInfo.xiaomiAppKey).thenReturn("xyz")
        Assert.assertFalse(handler.isAvailable)
    }

    @Test
    fun testIsAvailable_inValidAppId_inValidAppKey() {
        Mockito.`when`(manifestInfo.xiaomiAppID).thenReturn("")
        Mockito.`when`(manifestInfo.xiaomiAppKey).thenReturn(null)
        Assert.assertFalse(handler.isAvailable)
    }

    @Test
    fun testShouldInit_Returns_True() {
        Assert.assertTrue(handler.shouldInit(application.packageName))
    }

    @Test
    fun testShouldInit_Returns_False() {
        Assert.assertFalse(handler.shouldInit("XYZ"))
    }
}