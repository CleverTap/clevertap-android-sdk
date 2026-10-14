package com.clevertap.android.hms

import com.clevertap.android.hms.HmsConstants.MIN_CT_ANDROID_SDK_VERSION
import com.clevertap.android.hms.HmsConstants.HPS
import com.clevertap.android.hms.HmsConstants.HMS_SDK_NAME
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.pushnotification.CTPushProviderListener
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import io.mockk.every
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
        CleverTapAPI.setInstances(null)
        pushProvider = HmsPushProvider(ctPushProviderListener, application, cleverTapInstanceConfig)
        sdkHandler = mockk<IHmsSdkHandler>(relaxed = true)
        pushProvider.setHmsSdkHandler(sdkHandler)
    }

    @After
    fun tearDown() {
        CleverTapAPI.setInstances(null)
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
    fun testPushFlowUnaffectedWhenInstanceNotAvailable() {
        every { sdkHandler.isSupported } returns true
        every { sdkHandler.onNewToken() } returns "hms-token"

        Assert.assertTrue(pushProvider.isSupported)
        pushProvider.requestToken()

        verify(exactly = 1) { ctPushProviderListener.onNewToken("hms-token", HPS) }
    }

    @Test
    fun testRegisterSdkVersionSkipsInstanceOfAnotherAccount() {
        val otherAccountInstance = mockk<CleverTapAPI>(relaxed = true)
        CleverTapAPI.setInstances(hashMapOf("other-account-id" to otherAccountInstance))

        pushProvider.isSupported
        pushProvider.requestToken()

        verify(exactly = 0) { otherAccountInstance.setCustomSdkVersion(any(), any()) }
    }

    @Test
    fun testPushFlowUnaffectedWhenSetCustomSdkVersionThrows() {
        val cleverTapAPI = mockk<CleverTapAPI>(relaxed = true)
        every { cleverTapAPI.setCustomSdkVersion(any(), any()) } throws NoSuchMethodError("setCustomSdkVersion")
        every { sdkHandler.onNewToken() } returns "hms-token"
        CleverTapAPI.setInstances(hashMapOf(cleverTapInstanceConfig.accountId to cleverTapAPI))

        pushProvider.requestToken()

        verify(exactly = 1) { cleverTapAPI.setCustomSdkVersion(HMS_SDK_NAME, BuildConfig.VERSION_CODE) }
        verify(exactly = 1) { ctPushProviderListener.onNewToken("hms-token", HPS) }
    }

    @Test
    fun testRegisterSdkVersionFromRequestTokenWhenIsSupportedWasTooEarly() {
        val cleverTapAPI = mockk<CleverTapAPI>(relaxed = true)
        pushProvider.isSupported

        CleverTapAPI.setInstances(hashMapOf(cleverTapInstanceConfig.accountId to cleverTapAPI))
        pushProvider.requestToken()

        verify(exactly = 1) { cleverTapAPI.setCustomSdkVersion(HMS_SDK_NAME, BuildConfig.VERSION_CODE) }
    }

    @Test
    fun testRegisterSdkVersionOnEveryIsSupportedAndRequestTokenCall() {
        val cleverTapAPI = mockk<CleverTapAPI>(relaxed = true)
        CleverTapAPI.setInstances(hashMapOf(cleverTapInstanceConfig.accountId to cleverTapAPI))

        pushProvider.isSupported
        pushProvider.requestToken()

        verify(exactly = 2) { cleverTapAPI.setCustomSdkVersion(HMS_SDK_NAME, BuildConfig.VERSION_CODE) }
    }

    @Test
    fun testRegisterSdkVersionNotCalledFromConstructorOrIsAvailable() {
        val cleverTapAPI = mockk<CleverTapAPI>(relaxed = true)
        CleverTapAPI.setInstances(hashMapOf(cleverTapInstanceConfig.accountId to cleverTapAPI))
        val provider = HmsPushProvider(ctPushProviderListener, application, cleverTapInstanceConfig)
        provider.setHmsSdkHandler(sdkHandler)
        provider.isAvailable
        verify(exactly = 0) { cleverTapAPI.setCustomSdkVersion(any(), any()) }
    }

    @Test
    fun minSDKSupportVersionCode() {
        Assert.assertEquals(
            pushProvider.minSDKSupportVersionCode().toLong(),
            MIN_CT_ANDROID_SDK_VERSION.toLong()
        )
    }
}