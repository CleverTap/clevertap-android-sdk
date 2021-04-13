package com.clevertap.android.xps

import com.clevertap.android.sdk.ManifestInfo
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener
import com.clevertap.android.sdk.pushnotification.PushConstants.ANDROID_PLATFORM
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType.XPS
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.clevertap.android.xps.XpsConstants.MIN_CT_ANDROID_SDK_VERSION
import com.clevertap.android.xps.XpsTestConstants.Companion.MI_APP_ID
import com.clevertap.android.xps.XpsTestConstants.Companion.MI_APP_KEY
import com.clevertap.android.xps.XpsTestConstants.Companion.MI_TOKEN
import com.xiaomi.mipush.sdk.MiPushClient
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class XiaomiPushProviderTest : BaseTestCase() {

    private lateinit var ctPushProviderListener: CTPushProviderListener
    private lateinit var xiaomiPushProvider: XiaomiPushProvider
    private lateinit var sdkHandler: XiaomiSdkHandler
    private lateinit var manifestInfo: ManifestInfo

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        manifestInfo = Mockito.mock(ManifestInfo::class.java)
        Mockito.`when`(manifestInfo.xiaomiAppKey).thenReturn(MI_APP_KEY)
        Mockito.`when`(manifestInfo.xiaomiAppID).thenReturn(MI_APP_ID)

        //init provider listener
        ctPushProviderListener = Mockito.mock(CTPushProviderListener::class.java)
        sdkHandler = XiaomiSdkHandler(application, cleverTapInstanceConfig)
        sdkHandler.setManifestInfo(manifestInfo)
        //init push provider
        xiaomiPushProvider = XiaomiPushProvider(ctPushProviderListener, application, cleverTapInstanceConfig)
        xiaomiPushProvider.setMiSdkHandler(sdkHandler)
    }

    @Test
    fun testRequestToken() {
        Mockito.mockStatic(MiPushClient::class.java).use {
            Mockito.`when`(MiPushClient.getRegId(application)).thenReturn(MI_TOKEN)
            xiaomiPushProvider.requestToken()
            Mockito.verify(ctPushProviderListener).onNewToken(MI_TOKEN, XPS)
        }
    }

    @Test
    fun testIsAvailable_ReturnsFalse() {
        Mockito.`when`(manifestInfo.xiaomiAppID).thenReturn(null)
        Mockito.`when`(manifestInfo.xiaomiAppKey).thenReturn(null)
        Assert.assertFalse(xiaomiPushProvider.isAvailable)
    }

    @Test
    fun testIsAvailable_ReturnsTrue() {
        Mockito.`when`(manifestInfo.xiaomiAppID).thenReturn(MI_APP_KEY)
        Mockito.`when`(manifestInfo.xiaomiAppKey).thenReturn(MI_APP_ID)
        Assert.assertTrue(xiaomiPushProvider.isAvailable)
    }

    @Test
    fun testIsSupported() {
        Assert.assertTrue(xiaomiPushProvider.isSupported)
    }

    @Test
    fun testGetPlatform() {
        Assert.assertEquals(xiaomiPushProvider.platform.toLong(), ANDROID_PLATFORM.toLong())
    }

    @Test
    fun testGetPushType() {
        Assert.assertEquals(xiaomiPushProvider.pushType, XPS)
    }

    @Test
    fun minSDKSupportVersionCode() {
        Assert.assertEquals(
            xiaomiPushProvider.minSDKSupportVersionCode().toLong(),
            MIN_CT_ANDROID_SDK_VERSION.toLong()
        )
    }

    @Test
    fun testRegister_ValidConfigs() {
        sdkHandler.register(sdkHandler.appId(), sdkHandler.appKey())
        Assert.assertTrue(sdkHandler.isRegistered)
    }

    @Test
    fun testRegister_InValidConfigs() {
        assertFailsWith<RegistrationException> {
            sdkHandler.register(null, null)
        }
    }
}