package com.clevertap.android.hms

import com.clevertap.android.shared.test.BaseTestCase
import com.clevertap.android.shared.test.TestApplication
import com.huawei.agconnect.AGConnectOptions
import com.huawei.agconnect.AGConnectOptionsBuilder
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
    private lateinit var huaweiApi: HuaweiApiAvailability
    private var instance: HmsInstanceId? = null

    private var sdkHandler: HmsSdkHandler? = null
    private lateinit var agConnectOptionsSpy:AGConnectOptions

    @Before
    override fun setUp() {
        super.setUp()
        instance = Mockito.mock(HmsInstanceId::class.java)
        huaweiApi = Mockito.mock(HuaweiApiAvailability::class.java)
        agConnectOptionsSpy = Mockito.spy(AGConnectOptionsBuilder().build(appCtx))
        sdkHandler = HmsSdkHandler(application, cleverTapInstanceConfig,agConnectOptionsSpy)

    }

    @Test
    fun testAppId_Invalid() {
        Mockito.`when`(agConnectOptionsSpy.getString(HmsConstants.APP_ID_KEY)).thenThrow(RuntimeException("Something went wrong"))
        val appId = sdkHandler!!.appId()
        Assert.assertNull(appId)
    }

    @Test
    fun testAppId_Valid() {
        Mockito.`when`(agConnectOptionsSpy.getString(HmsConstants.APP_ID_KEY)).thenReturn(HmsTestConstants.HMS_APP_ID)
        val appId = sdkHandler!!.appId()
        Assert.assertNotNull(appId)
    }

    @Test
    fun testIsAvailable_Returns_False() {
        Mockito.`when`(agConnectOptionsSpy.getString(HmsConstants.APP_ID_KEY)).thenThrow(RuntimeException("Something Went Wrong"))
        Assert.assertFalse(sdkHandler!!.isAvailable)
    }

    @Test
    fun testIsAvailable_Returns_True() {
        Mockito.`when`(agConnectOptionsSpy.getString(HmsConstants.APP_ID_KEY)).thenReturn(HmsTestConstants.HMS_APP_ID)
        Assert.assertTrue(sdkHandler!!.isAvailable)
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
            Mockito.`when`(agConnectOptionsSpy.getString(HmsConstants.APP_ID_KEY)).thenReturn(HmsTestConstants.HMS_APP_ID)
            Mockito.`when`(instance!!.getToken(HmsTestConstants.HMS_APP_ID, HmsConstants.HCM_SCOPE)).thenThrow(RuntimeException("Something went wrong"))
            Mockito.mockStatic(HmsInstanceId::class.java).use { mocked ->
                Mockito.`when`(HmsInstanceId.getInstance(application)).thenReturn(instance)
                val token = sdkHandler!!.onNewToken()
                Assert.assertNull(token)
            }
        }
        catch (e: Exception) {
        }
    }

    @Test
    fun testNewToken_Invalid_AppId() {
        Mockito.`when`(agConnectOptionsSpy.getString(HmsConstants.APP_ID_KEY)).thenReturn(null)
        val token = sdkHandler!!.onNewToken()
        Assert.assertNull(token)
    }

    @Test
    fun testNewToken_Success() {
        try {
            Mockito.`when`(agConnectOptionsSpy.getString(HmsConstants.APP_ID_KEY)).thenReturn(HmsTestConstants.HMS_APP_ID)
            Mockito.`when`(instance!!.getToken(HmsTestConstants.HMS_APP_ID, HmsConstants.HCM_SCOPE)).thenReturn(HmsTestConstants.HMS_TOKEN)
            Mockito.mockStatic(HmsInstanceId::class.java).use { mocked ->
                Mockito.`when`(HmsInstanceId.getInstance(application)).thenReturn(instance)
                val token = sdkHandler!!.onNewToken()
                Assert.assertEquals(HmsTestConstants.HMS_TOKEN, token)
            }
        }
        catch (e: Exception) {
            //do nothing
        }
    }
}