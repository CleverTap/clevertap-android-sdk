package com.clevertap.android.sdk.pushnotification.fcm

import com.clevertap.android.sdk.pushnotification.CTPushProviderListener
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FcmPushProviderTest : BaseTestCase() {

    private lateinit var provider: FcmPushProvider
    private lateinit var ctPushProviderListener: CTPushProviderListener
    private lateinit var sdkHandler: IFcmSdkHandler

    override fun setUp() {
        super.setUp()
        ctPushProviderListener = mockk(relaxed = true)
        provider = FcmPushProvider(ctPushProviderListener, application, cleverTapInstanceConfig)
        sdkHandler = mockk<FcmSdkHandlerImpl>(relaxed = true)
        provider.setHandler(sdkHandler)
    }

    @Test
    fun testGetPushType() {
        provider.pushType
        verify(exactly = 1) { sdkHandler.pushType }
    }

    @Test
    fun testIsAvailable() {
        provider.isAvailable
        verify(exactly = 1) { sdkHandler.isAvailable }
    }

    @Test
    fun isSupported() {
        provider.isSupported
        verify(exactly = 1) { sdkHandler.isSupported }
    }

    @Test
    fun testMinSDKSupportVersionCode() {
        Assert.assertEquals(0, provider.minSDKSupportVersionCode())
    }

    @Test
    fun requestToken() {
        provider.requestToken()
        verify(exactly = 1) { sdkHandler.requestToken() }
    }
}
