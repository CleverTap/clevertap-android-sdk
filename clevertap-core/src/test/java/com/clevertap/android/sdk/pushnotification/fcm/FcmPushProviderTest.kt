package com.clevertap.android.sdk.pushnotification.fcm

import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.MockDeviceInfo
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener
import com.clevertap.android.sdk.pushnotification.PushConstants.ANDROID_PLATFORM
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import org.junit.*
import org.junit.runner.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class FcmPushProviderTest : BaseTestCase() {

    private lateinit var provider: FcmPushProvider
    private lateinit var ctPushProviderListener: CTPushProviderListener
    private lateinit var sdkHandler: IFcmSdkHandler

    @Before
    override fun setUp() {
        super.setUp()
        ctPushProviderListener = mock(CTPushProviderListener::class.java)
        provider = FcmPushProvider(ctPushProviderListener, application, cleverTapInstanceConfig)
        sdkHandler = mock(FcmSdkHandlerImpl::class.java)
        provider.setHandler(sdkHandler)
    }

    @Test
    fun testGetPlatform() {
        Assert.assertEquals(ANDROID_PLATFORM, provider.platform)
    }

    @Test
    fun testGetPushType() {
        provider.pushType
        verify(sdkHandler, times(1)).pushType
    }

    @Test
    fun testIsAvailable() {
        provider.isAvailable
        verify(sdkHandler, times(1)).isAvailable
    }

    @Test
    fun isSupported() {
        provider.isSupported
        verify(sdkHandler, times(1)).isSupported
    }

    @Test
    fun testMinSDKSupportVersionCode() {
        Assert.assertEquals(0, provider.minSDKSupportVersionCode())
    }

    @Test
    fun requestToken() {
        provider.requestToken()
        verify(sdkHandler, times(1)).requestToken()
    }
}