package com.clevertap.android.hms

import com.clevertap.android.hms.HmsConstants.MIN_CT_ANDROID_SDK_VERSION
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener
import com.clevertap.android.sdk.pushnotification.PushConstants.ANDROID_PLATFORM
import com.clevertap.android.sdk.pushnotification.PushConstants.PushType.HPS
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import org.junit.*
import org.junit.runner.*
import org.mockito.AdditionalMatchers.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class HmsPushProviderTest : BaseTestCase() {

    private lateinit var ctPushProviderListener: CTPushProviderListener
    private lateinit var pushProvider: HmsPushProvider
    private lateinit var sdkHandler: IHmsSdkHandler

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        ctPushProviderListener = mock(CTPushProviderListener::class.java)
        pushProvider = HmsPushProvider(ctPushProviderListener, application, cleverTapInstanceConfig)
        sdkHandler = mock(IHmsSdkHandler::class.java)
        pushProvider.setHmsSdkHandler(sdkHandler)
    }

    @Test
    fun testRequestToken() {
        pushProvider.requestToken()
        verify(sdkHandler, times(1)).onNewToken()
        verify(ctPushProviderListener, times(1)).onNewToken(or(isNull(), anyString()), eq(HPS))
    }

    @Test
    fun testIsAvailable() {
        pushProvider.isAvailable
        verify(sdkHandler, times(1)).isAvailable
    }

    @Test
    fun testIsSupported() {
        pushProvider.isSupported
        verify(sdkHandler, times(1)).isSupported
    }

    @Test
    fun testGetPlatform() {
        Assert.assertEquals(pushProvider.platform.toLong(), ANDROID_PLATFORM.toLong())
    }

    @Test
    fun testGetPushType() {
        Assert.assertEquals(pushProvider.pushType, HPS)
    }

    @Test
    fun minSDKSupportVersionCode() {
        Assert.assertEquals(
            pushProvider.minSDKSupportVersionCode().toLong(),
            MIN_CT_ANDROID_SDK_VERSION.toLong()
        )
    }
}