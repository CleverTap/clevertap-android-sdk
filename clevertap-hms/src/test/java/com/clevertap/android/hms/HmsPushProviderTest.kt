package com.clevertap.android.hms

import com.clevertap.android.sdk.pushnotification.CTPushProviderListener
import com.clevertap.android.sdk.pushnotification.PushConstants
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class HmsPushProviderTest : BaseTestCase() {
    private lateinit var ctPushProviderListener: CTPushProviderListener
    private var pushProvider: HmsPushProvider? = null
    private var sdkHandler: TestHmsSdkHandler? = null

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        ctPushProviderListener = Mockito.mock(CTPushProviderListener::class.java)
        pushProvider = HmsPushProvider(ctPushProviderListener)
        sdkHandler = TestHmsSdkHandler()
        Mockito.`when`(ctPushProviderListener.context()).thenReturn(application)
        Mockito.`when`(ctPushProviderListener.config()).thenReturn(cleverTapInstanceConfig)
        pushProvider!!.setHmsSdkHandler(sdkHandler)
    }

    @Test
    fun testRequestToken() {
        pushProvider!!.requestToken()
        Mockito.verify(ctPushProviderListener).onNewToken(HmsTestConstants.HMS_TOKEN, PushConstants.PushType.HPS)
    }

    @Test
    fun testIsAvailable() {
        sdkHandler!!.setAvailable(false)
        Assert.assertFalse(pushProvider!!.isAvailable)
        sdkHandler!!.setAvailable(true)
        Assert.assertTrue(pushProvider!!.isAvailable)
    }

    @Test
    fun testIsSupported() {
        sdkHandler!!.isSupported = true
        Assert.assertTrue(pushProvider!!.isSupported)
        sdkHandler!!.isSupported = false
        Assert.assertFalse(pushProvider!!.isSupported)
    }
}