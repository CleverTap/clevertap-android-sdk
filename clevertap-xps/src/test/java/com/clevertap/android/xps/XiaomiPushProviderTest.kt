package com.clevertap.android.xps

import com.clevertap.android.sdk.ManifestInfo
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener
import com.clevertap.android.sdk.pushnotification.PushConstants
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.xiaomi.mipush.sdk.MiPushClient
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class XiaomiPushProviderTest : BaseTestCase() {
    private lateinit var ctPushProviderListener: CTPushProviderListener
    private var xiaomiPushProvider: XiaomiPushProvider? = null
    private var sdkHandler: XiaomiSdkHandler? = null
    private var manifestInfo: ManifestInfo? = null

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        manifestInfo = Mockito.mock(ManifestInfo::class.java)
        Mockito.`when`(manifestInfo!!.xiaomiAppKey).thenReturn(XpsTestConstants.MI_APP_KEY)
        Mockito.`when`(manifestInfo!!.xiaomiAppID).thenReturn(XpsTestConstants.MI_APP_ID)

        //init provider listener
        ctPushProviderListener = Mockito.mock(CTPushProviderListener::class.java)
        Mockito.`when`(ctPushProviderListener.context()).thenReturn(application)
        Mockito.`when`(ctPushProviderListener.config()).thenReturn(cleverTapInstanceConfig)
        sdkHandler = XiaomiSdkHandler(ctPushProviderListener)
        sdkHandler!!.setManifestInfo(manifestInfo)
        //init push provider
        xiaomiPushProvider = XiaomiPushProvider(ctPushProviderListener)
        xiaomiPushProvider!!.setMiSdkHandler(sdkHandler!!)

    }

    @Test
    fun testRequestToken() {
        Mockito.mockStatic(MiPushClient::class.java).use {
            Mockito.`when`(MiPushClient.getRegId(application)).thenReturn(XpsTestConstants.MI_TOKEN)
            xiaomiPushProvider!!.requestToken()
            Mockito.verify(ctPushProviderListener).onNewToken(XpsTestConstants.MI_TOKEN, PushConstants.PushType.XPS)
        }

    }

    @Test
    fun testIsAvailable_ReturnsFalse() {
        Mockito.`when`(manifestInfo!!.xiaomiAppID).thenReturn(null)
        Mockito.`when`(manifestInfo!!.xiaomiAppKey).thenReturn(null)
        Assert.assertFalse(xiaomiPushProvider!!.isAvailable)
    }

    @Test
    fun testIsAvailable_ReturnsTrue() {
        Mockito.`when`(manifestInfo!!.xiaomiAppID).thenReturn(XpsTestConstants.MI_APP_KEY)
        Mockito.`when`(manifestInfo!!.xiaomiAppKey).thenReturn(XpsTestConstants.MI_APP_ID)
        Assert.assertTrue(xiaomiPushProvider!!.isAvailable)
    }

    @Test
    fun testIsSupported() {
        Assert.assertTrue(xiaomiPushProvider!!.isSupported)
    }

    @Test
    fun testGetPlatform() {
        Assert.assertEquals(xiaomiPushProvider!!.platform.toLong(), PushConstants.ANDROID_PLATFORM.toLong())
    }

    @Test
    fun testGetPushType() {
        Assert.assertEquals(xiaomiPushProvider!!.pushType, PushConstants.PushType.XPS)
    }

    @Test
    fun minSDKSupportVersionCode() {
        Assert.assertEquals(xiaomiPushProvider!!.minSDKSupportVersionCode().toLong(), XpsConstants.MIN_CT_ANDROID_SDK_VERSION.toLong())
    }

    @Test
    fun testRegister_ValidConfigs() {
        sdkHandler!!.register(sdkHandler!!.appId(), sdkHandler!!.appKey())
        Assert.assertTrue(sdkHandler!!.isRegistered)
    }

    @Test
    fun testRegister_InValidConfigs() {
        assertFailsWith<RegistrationException> {
            sdkHandler!!.register(null, null)
        }
    }

    @After
    fun tearDown() {
        xiaomiPushProvider = null
        sdkHandler = null
    }
}