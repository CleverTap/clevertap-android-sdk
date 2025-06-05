package com.clevertap.android.hms

import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.huawei.agconnect.AGConnectOptions
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.api.HuaweiApiAvailability
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class HmsHandlerTest : BaseTestCase() {
    private lateinit var huaweiApi: HuaweiApiAvailability
    private lateinit var instance: HmsInstanceId

    private lateinit var sdkHandler: HmsSdkHandler
    private lateinit var agConnectOptionsSpy:AGConnectOptions

    @Before
    override fun setUp() {
        super.setUp()
        instance = mockk<HmsInstanceId>(relaxed = true)
        huaweiApi = mockk<HuaweiApiAvailability>(relaxed = true)
        agConnectOptionsSpy = spyk(AGConnectOptionsBuilder().build(appCtx))
        sdkHandler = HmsSdkHandler(application, cleverTapInstanceConfig,agConnectOptionsSpy)

    }

    @Test
    fun testAppId_Invalid() {
        every { agConnectOptionsSpy.getString(HmsConstants.APP_ID_KEY) } throws RuntimeException("Something went wrong")
        val appId = sdkHandler.appId()
        Assert.assertNull(appId)
    }

    @Test
    fun testAppId_Valid() {
        every { agConnectOptionsSpy.getString(HmsConstants.APP_ID_KEY) } returns HmsTestConstants.HMS_APP_ID
        val appId = sdkHandler.appId()
        Assert.assertNotNull(appId)
    }

    @Test
    fun testIsAvailable_Returns_False() {
        every { agConnectOptionsSpy.getString(HmsConstants.APP_ID_KEY) } throws RuntimeException("Something Went Wrong")
        Assert.assertFalse(sdkHandler.isAvailable)
    }

    @Test
    fun testIsAvailable_Returns_True() {
        every { agConnectOptionsSpy.getString(HmsConstants.APP_ID_KEY) } returns HmsTestConstants.HMS_APP_ID
        Assert.assertTrue(sdkHandler.isAvailable)
    }

    @Test
    fun testIsSupported_Returns_False() {
        every { huaweiApi.isHuaweiMobileNoticeAvailable(application) } returns 1
        mockkStatic(HuaweiApiAvailability::class) {
            every { HuaweiApiAvailability.getInstance() } returns huaweiApi
            Assert.assertFalse(sdkHandler.isSupported)
        }
    }

    @Test
    fun testIsSupported_Returns_False_Exception() {
        huaweiApi = mockk<HuaweiApiAvailability>()
        every { huaweiApi.isHuaweiMobileNoticeAvailable(application) } throws RuntimeException("Something went wrong")
        mockkStatic(HuaweiApiAvailability::class) {
            every { HuaweiApiAvailability.getInstance() } returns huaweiApi
            Assert.assertFalse(sdkHandler.isSupported)
        }
    }

    @Test
    fun testIsSupported_Returns_True() {
        huaweiApi = mockk<HuaweiApiAvailability>()
        every { huaweiApi.isHuaweiMobileNoticeAvailable(application) } returns 0
        mockkStatic(HuaweiApiAvailability::class) {
            every { HuaweiApiAvailability.getInstance() } returns huaweiApi
            Assert.assertTrue(sdkHandler.isSupported)
        }
    }

    @Test
    fun testNewToken_Exception() {
        every { agConnectOptionsSpy.getString(HmsConstants.APP_ID_KEY) } returns HmsTestConstants.HMS_APP_ID
        every { instance.getToken(HmsTestConstants.HMS_APP_ID, HmsConstants.HCM_SCOPE) } throws
                RuntimeException("Something went wrong")
        mockkStatic(HmsInstanceId::class) {
            every { HmsInstanceId.getInstance(application) } returns instance
            val token = sdkHandler.onNewToken()
                Assert.assertNull(token)
            }
    }

    @Test
    fun testNewToken_Invalid_AppId() {
        every { agConnectOptionsSpy.getString(HmsConstants.APP_ID_KEY) } returns null
        val token = sdkHandler.onNewToken()
        Assert.assertNull(token)
    }

    @Test
    fun testNewToken_Success() {
        every { agConnectOptionsSpy.getString(HmsConstants.APP_ID_KEY) } returns HmsTestConstants.HMS_APP_ID
        every {
            instance.getToken(
                HmsTestConstants.HMS_APP_ID,
                HmsConstants.HCM_SCOPE
            )
        } returns HmsTestConstants.HMS_TOKEN
        mockkStatic(HmsInstanceId::class) {
            every { HmsInstanceId.getInstance(application) } returns instance
            val token = sdkHandler.onNewToken()
                Assert.assertEquals(HmsTestConstants.HMS_TOKEN, token)
            }
        }
}