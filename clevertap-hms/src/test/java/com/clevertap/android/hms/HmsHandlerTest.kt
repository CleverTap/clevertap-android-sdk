package com.clevertap.android.hms

import com.clevertap.android.sdk.Application
import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.huawei.agconnect.config.AGConnectServicesConfig
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.api.HuaweiApiAvailability
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestApplication::class)
class HmsHandlerTest : BaseTestCase() {
    private var config: AGConnectServicesConfig? = null
    private lateinit var huaweiApi: HuaweiApiAvailability
    private var instance: HmsInstanceId? = null
    private var sdkHandler: HmsSdkHandler? = null
    @Before
    override fun setUp() {
        super.setUp()
        sdkHandler = HmsSdkHandler(application, cleverTapInstanceConfig)
        instance = Mockito.mock(HmsInstanceId::class.java)
        config = Mockito.mock(AGConnectServicesConfig::class.java)
        huaweiApi = Mockito.mock(HuaweiApiAvailability::class.java)
    }

    @Test
    fun testAppId_Invalid() {
        Mockito.`when`(config!!.getString(HmsConstants.APP_ID_KEY)).thenThrow(RuntimeException("Something went wrong"))
        Mockito.mockStatic(AGConnectServicesConfig::class.java).use { mocked ->
            Mockito.`when`(AGConnectServicesConfig.fromContext(application)).thenReturn(config)
            val appId = sdkHandler!!.appId()
            Assert.assertNull(appId)
        }
    }

    @Test
    fun testAppId_Valid() {
        Mockito.`when`(config!!.getString(HmsConstants.APP_ID_KEY)).thenReturn(HmsTestConstants.HMS_APP_ID)
        Mockito.mockStatic(AGConnectServicesConfig::class.java).use { mocked ->
            Mockito.`when`(AGConnectServicesConfig.fromContext(application)).thenReturn(config)
            val appId = sdkHandler!!.appId()
            Assert.assertNotNull(appId)
        }
    }

    @Test
    fun testIsAvailable_Returns_False() {
        Mockito.`when`(config!!.getString(HmsConstants.APP_ID_KEY)).thenThrow(RuntimeException("Something Went Wrong"))
        Mockito.mockStatic(AGConnectServicesConfig::class.java).use { mocked ->
            Mockito.`when`(AGConnectServicesConfig.fromContext(application)).thenReturn(config)
            Assert.assertFalse(sdkHandler!!.isAvailable)
        }
    }

    @Test
    fun testIsAvailable_Returns_True() {
        val config = Mockito.mock(AGConnectServicesConfig::class.java)
        Mockito.`when`(config.getString(HmsConstants.APP_ID_KEY)).thenReturn(HmsTestConstants.HMS_APP_ID)
        Mockito.mockStatic(AGConnectServicesConfig::class.java).use { mocked ->
            Mockito.`when`(AGConnectServicesConfig.fromContext(application)).thenReturn(config)
            Assert.assertTrue(sdkHandler!!.isAvailable)
        }
    }

    @Test
    fun testIsSupported_Returns_False() {
        Mockito.`when`(huaweiApi.isHuaweiMobileNoticeAvailable(application)).thenReturn(1)
        Mockito.mockStatic(HuaweiApiAvailability::class.java).use { mocked ->
            Mockito.`when`(HuaweiApiAvailability.getInstance()).thenReturn(huaweiApi)
            Assert.assertFalse(sdkHandler!!.isSupported)
        }
    }

    @Test
    fun testIsSupported_Returns_False_Exception() {
        huaweiApi = Mockito.mock(HuaweiApiAvailability::class.java)
        Mockito.`when`(huaweiApi.isHuaweiMobileNoticeAvailable(application))
                .thenThrow(RuntimeException("Something went wrong"))
        Mockito.mockStatic(HuaweiApiAvailability::class.java).use { mocked ->
            Mockito.`when`(HuaweiApiAvailability.getInstance()).thenReturn(huaweiApi)
            Assert.assertFalse(sdkHandler!!.isSupported)
        }
    }

    @Test
    fun testIsSupported_Returns_True() {
        huaweiApi = Mockito.mock(HuaweiApiAvailability::class.java)
        Mockito.`when`(huaweiApi.isHuaweiMobileNoticeAvailable(application)).thenReturn(0)
        Mockito.mockStatic(HuaweiApiAvailability::class.java).use { mocked ->
            Mockito.`when`(HuaweiApiAvailability.getInstance()).thenReturn(huaweiApi)
            Assert.assertTrue(sdkHandler!!.isSupported)
        }
    }

    @Test
    fun testNewToken_Exception() {
        try {
            Mockito.`when`(config!!.getString(HmsConstants.APP_ID_KEY)).thenReturn(HmsTestConstants.HMS_APP_ID)
            Mockito.`when`(instance!!.getToken(HmsTestConstants.HMS_APP_ID, HmsConstants.HCM_SCOPE))
                    .thenThrow(RuntimeException("Something went wrong"))
            Mockito.mockStatic(HmsInstanceId::class.java).use { mocked ->
                Mockito.`when`(HmsInstanceId.getInstance(application)).thenReturn(instance)
                Mockito.mockStatic(AGConnectServicesConfig::class.java).use { mocked1 ->
                    Mockito.`when`(AGConnectServicesConfig.fromContext(application)).thenReturn(config)
                    val token = sdkHandler!!.onNewToken()
                    Assert.assertNull(token)
                }
            }
        } catch (e: Exception) {
        }
    }

    @Test
    fun testNewToken_Invalid_AppId() {
        Mockito.`when`(config!!.getString(HmsConstants.APP_ID_KEY)).thenReturn(null)
        Mockito.mockStatic(AGConnectServicesConfig::class.java).use { mocked1 ->
            Mockito.`when`(AGConnectServicesConfig.fromContext(application)).thenReturn(config)
            val token = sdkHandler!!.onNewToken()
            Assert.assertNull(token)
        }
    }

    @Test
    fun testNewToken_Success() {
        try {
            Mockito.`when`(config!!.getString(HmsConstants.APP_ID_KEY)).thenReturn(HmsTestConstants.HMS_APP_ID)
            Mockito.`when`(instance!!.getToken(HmsTestConstants.HMS_APP_ID, HmsConstants.HCM_SCOPE)).thenReturn(HmsTestConstants.HMS_TOKEN)
            Mockito.mockStatic(HmsInstanceId::class.java).use { mocked ->
                Mockito.`when`(HmsInstanceId.getInstance(application)).thenReturn(instance)
                Mockito.mockStatic(AGConnectServicesConfig::class.java).use { mocked1 ->
                    Mockito.`when`(AGConnectServicesConfig.fromContext(application)).thenReturn(config)
                    val token = sdkHandler!!.onNewToken()
                    Assert.assertEquals(token, HmsTestConstants.HMS_TOKEN)
                }
            }
        } catch (e: Exception) {
            //do nothing
        }
    }
}