package com.clevertap.android.hms

import com.clevertap.android.hms.HmsConstants.APP_ID_KEY
import com.clevertap.android.hms.HmsConstants.HCM_SCOPE
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.huawei.agconnect.AGConnectInstance
import com.huawei.agconnect.AGConnectOptions
import com.huawei.agconnect.AGConnectOptionsBuilder
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.api.HuaweiApiAvailability
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        every { agConnectOptionsSpy.getString(APP_ID_KEY) } throws RuntimeException("Something went wrong")
        val appId = sdkHandler.appId()
        Assert.assertNull(appId)
    }

    @Test
    fun testAppId_Valid() {
        every { agConnectOptionsSpy.getString(APP_ID_KEY) } returns HmsTestConstants.HMS_APP_ID
        val appId = sdkHandler.appId()
        Assert.assertNotNull(appId)
    }

    @Test
    fun testIsAvailable_Returns_False() {
        every { agConnectOptionsSpy.getString(APP_ID_KEY) } throws RuntimeException("Something Went Wrong")
        Assert.assertFalse(sdkHandler.isAvailable)
    }

    @Test
    fun testIsAvailable_Returns_True() {
        every { agConnectOptionsSpy.getString(APP_ID_KEY) } returns HmsTestConstants.HMS_APP_ID
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
        every { agConnectOptionsSpy.getString(APP_ID_KEY) } returns HmsTestConstants.HMS_APP_ID
        every { instance.getToken(HmsTestConstants.HMS_APP_ID, HCM_SCOPE) } throws
                RuntimeException("Something went wrong")
        mockkStatic(HmsInstanceId::class) {
            every { HmsInstanceId.getInstance(application) } returns instance
            val token = sdkHandler.onNewToken()
                Assert.assertNull(token)
            }
    }

    // Add these test cases to your existing HmsHandlerTest.kt file

    @Test
    fun `appId should initialize options when options is null and return app id from existing AGConnect instance`() {
        // Arrange
        val mockOptions = mockk<AGConnectOptions>()
        val mockInstance = mockk<AGConnectInstance>()

        mockkStatic(AGConnectInstance::class)
        every { AGConnectInstance.getInstance() } returns mockInstance
        every { mockInstance.options } returns mockOptions
        every { mockOptions.getString(APP_ID_KEY) } returns "test-app-id"

        val handler = HmsSdkHandler(application, cleverTapInstanceConfig)

        // Act
        val result = handler.appId()

        // Assert
        assertEquals("test-app-id", result)
        verify { AGConnectInstance.getInstance() }
        verify { mockInstance.options }
    }

    @Test
    fun `appId should fallback to AGConnectOptionsBuilder when existing instance is null`() {
        // Arrange
        val mockOptions = mockk<AGConnectOptions>()

        mockkStatic(AGConnectInstance::class)
        mockkConstructor(AGConnectOptionsBuilder::class)

        every { AGConnectInstance.getInstance() } returns null
        every { anyConstructed<AGConnectOptionsBuilder>().build(application) } returns mockOptions
        every { mockOptions.getString(APP_ID_KEY) } returns "fallback-app-id"

        val handler = HmsSdkHandler(application, cleverTapInstanceConfig)

        // Act
        val result = handler.appId()

        // Assert
        assertEquals("fallback-app-id", result)
        verify { AGConnectInstance.getInstance() }
        verify { anyConstructed<AGConnectOptionsBuilder>().build(application) }
    }

    @Test
    fun `appId should fallback to AGConnectOptionsBuilder when existing instance options is null`() {
        // Arrange
        val mockOptions = mockk<AGConnectOptions>()
        val mockInstance = mockk<AGConnectInstance>()

        mockkStatic(AGConnectInstance::class)
        mockkConstructor(AGConnectOptionsBuilder::class)

        every { AGConnectInstance.getInstance() } returns mockInstance
        every { mockInstance.options } returns null
        every { anyConstructed<AGConnectOptionsBuilder>().build(application) } returns mockOptions
        every { mockOptions.getString(APP_ID_KEY) } returns "builder-app-id"

        val handler = HmsSdkHandler(application, cleverTapInstanceConfig)

        // Act
        val result = handler.appId()

        // Assert
        assertEquals("builder-app-id", result)
        verify { AGConnectInstance.getInstance() }
        verify { mockInstance.options }
        verify { anyConstructed<AGConnectOptionsBuilder>().build(application) }
    }

    @Test
    fun `appId should handle exception during AGConnect instance retrieval and fallback to builder`() {
        // Arrange
        val mockOptions = mockk<AGConnectOptions>()

        mockkStatic(AGConnectInstance::class)
        mockkConstructor(AGConnectOptionsBuilder::class)

        every { AGConnectInstance.getInstance() } throws RuntimeException("AGConnect not initialized")
        every { anyConstructed<AGConnectOptionsBuilder>().build(application) } returns mockOptions
        every { mockOptions.getString(APP_ID_KEY) } returns "exception-recovery-app-id"

        val handler = HmsSdkHandler(application, cleverTapInstanceConfig)

        // Act
        val result = handler.appId()

        // Assert
        assertEquals("exception-recovery-app-id", result)
    }

    @Test
    fun `appId should handle exception during AGConnectOptionsBuilder`() {
        // Arrange
        mockkStatic(AGConnectInstance::class)
        mockkConstructor(AGConnectOptionsBuilder::class)

        every { AGConnectInstance.getInstance() } returns null
        every { anyConstructed<AGConnectOptionsBuilder>().build(application) } throws RuntimeException("Build failed")

        val handler = HmsSdkHandler(application, cleverTapInstanceConfig)

        // Act
        val result = handler.appId()

        // Assert
        assertNull(result)
    }

    @Test
    fun `isAvailable should trigger options initialization and return true when app id exists`() {
        // Arrange
        val mockOptions = mockk<AGConnectOptions>()
        val mockInstance = mockk<AGConnectInstance>()

        mockkStatic(AGConnectInstance::class)
        every { AGConnectInstance.getInstance() } returns mockInstance
        every { mockInstance.options } returns mockOptions
        every { mockOptions.getString(APP_ID_KEY) } returns "test-app-id"

        val handler = HmsSdkHandler(application, cleverTapInstanceConfig)

        // Act
        val result = handler.isAvailable()

        // Assert
        assertTrue(result)
        verify { AGConnectInstance.getInstance() } // Confirms initialization was triggered
    }

    @Test
    fun `isAvailable should trigger options initialization and return false when app id is empty`() {
        // Arrange
        val mockOptions = mockk<AGConnectOptions>()
        val mockInstance = mockk<AGConnectInstance>()

        mockkStatic(AGConnectInstance::class)
        every { AGConnectInstance.getInstance() } returns mockInstance
        every { mockInstance.options } returns mockOptions
        every { mockOptions.getString(APP_ID_KEY) } returns ""

        val handler = HmsSdkHandler(application, cleverTapInstanceConfig)

        // Act
        val result = handler.isAvailable()

        // Assert
        assertFalse(result)
        verify { AGConnectInstance.getInstance() } // Confirms initialization was triggered
    }

    @Test
    fun `onNewToken should trigger options initialization and use app id for token retrieval`() {
        // Arrange
        val mockOptions = mockk<AGConnectOptions>()
        val mockInstance = mockk<AGConnectInstance>()
        val mockHmsInstanceId = mockk<HmsInstanceId>()

        mockkStatic(AGConnectInstance::class)
        mockkStatic(HmsInstanceId::class)

        every { AGConnectInstance.getInstance() } returns mockInstance
        every { mockInstance.options } returns mockOptions
        every { mockOptions.getString(APP_ID_KEY) } returns "token-app-id"
        every { HmsInstanceId.getInstance(application) } returns mockHmsInstanceId
        every { mockHmsInstanceId.getToken("token-app-id", HCM_SCOPE) } returns "test-token"

        val handler = HmsSdkHandler(application, cleverTapInstanceConfig)

        // Act
        val result = handler.onNewToken()

        // Assert
        assertEquals("test-token", result)
        verify { AGConnectInstance.getInstance() } // Confirms initialization was triggered
        verify { mockHmsInstanceId.getToken("token-app-id", HCM_SCOPE) }
    }

    @Test
    fun testNewToken_Invalid_AppId() {
        every { agConnectOptionsSpy.getString(APP_ID_KEY) } returns null
        val token = sdkHandler.onNewToken()
        Assert.assertNull(token)
    }

    @Test
    fun testNewToken_Success() {
        every { agConnectOptionsSpy.getString(APP_ID_KEY) } returns HmsTestConstants.HMS_APP_ID
        every {
            instance.getToken(
                HmsTestConstants.HMS_APP_ID,
                HCM_SCOPE
            )
        } returns HmsTestConstants.HMS_TOKEN
        mockkStatic(HmsInstanceId::class) {
            every { HmsInstanceId.getInstance(application) } returns instance
            val token = sdkHandler.onNewToken()
                Assert.assertEquals(HmsTestConstants.HMS_TOKEN, token)
            }
        }
}