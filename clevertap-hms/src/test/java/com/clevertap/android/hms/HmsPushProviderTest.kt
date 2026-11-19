package com.clevertap.android.hms

import com.clevertap.android.hms.HmsConstants.MIN_CT_ANDROID_SDK_VERSION
import com.clevertap.android.hms.HmsConstants.HPS
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import io.mockk.mockk
import io.mockk.verify
import org.junit.*
import org.junit.runner.*
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
        ctPushProviderListener = mockk<CTPushProviderListener>(relaxed = true)
        pushProvider = HmsPushProvider(ctPushProviderListener, application, cleverTapInstanceConfig)
        sdkHandler = mockk<IHmsSdkHandler>(relaxed = true)
        pushProvider.setHmsSdkHandler(sdkHandler)
    }

    @Test
    fun testRequestToken() {
        pushProvider.requestToken()
        verify(exactly = 1) { sdkHandler.onNewToken() }
        verify(exactly = 1) { ctPushProviderListener.onNewToken(or(isNull(), any()), eq(HPS)) }
    }

    @Test
    fun testIsAvailable() {
        pushProvider.isAvailable
        verify(exactly = 1) { sdkHandler.isAvailable }
    }

    @Test
    fun testIsSupported() {
        pushProvider.isSupported
        verify(exactly = 1) { sdkHandler.isSupported }
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