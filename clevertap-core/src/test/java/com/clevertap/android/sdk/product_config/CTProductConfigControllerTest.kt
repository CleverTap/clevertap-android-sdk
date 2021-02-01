package com.clevertap.android.sdk.product_config

import com.clevertap.android.sdk.BaseAnalyticsManager
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.MockCallbackManager
import com.clevertap.android.sdk.MockDeviceInfo
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CTProductConfigControllerTest : BaseTestCase() {

    private lateinit var mProductConfigController: CTProductConfigController
    private val guid = "1212121221"
    private lateinit var coreMetaData: CoreMetaData
    private lateinit var analyticsManager: BaseAnalyticsManager
    private lateinit var callbackManager: BaseCallbackManager
    private lateinit var productConfigSettings: ProductConfigSettings
    private lateinit var deviceInfo: DeviceInfo

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        coreMetaData = CoreMetaData()
        analyticsManager = Mockito.mock(BaseAnalyticsManager::class.java)
        deviceInfo = MockDeviceInfo(application, cleverTapInstanceConfig, guid, coreMetaData)
        callbackManager = MockCallbackManager()
        productConfigSettings = Mockito.mock(ProductConfigSettings::class.java)
        mProductConfigController = CTProductConfigController(
            application,
            guid,
            cleverTapInstanceConfig,
            analyticsManager,
            coreMetaData,
            callbackManager
        )
    }

    @Throws(Exception::class)
    fun tearDown() {
    }

    @Test
    fun testSetDefaultConfig() {}

    @Test
    fun testActivate() {}
    @Test
    fun testFetch() {
        mProductConfigController.fetch()
        Mockito.`when`(productConfigSettings.nextFetchIntervalInSeconds).thenReturn(0)
        Mockito.verify(analyticsManager).sendFetchEvent(Mockito.any())
        Assert.assertTrue(coreMetaData.isProductConfigRequested)
    }

    @Test
    fun testTestFetch() {}

    @Test
    fun testFetchAndActivate() {}

    @Test
    fun testFetchProductConfig() {}

    @Test
    fun testGetBoolean() {}

    @Test
    fun testGetDouble() {}

    @Test
    fun testGetLastFetchTimeStampInMillis() {}

    @Test
    fun testGetLong() {}

    @Test
    fun testGetString() {}

    @Test
    fun testIsInitialized() {}

    @Test
    fun testOnFetchFailed() {}

    @Test
    fun testOnFetchSuccess() {}

    @Test
    fun testReset() {}

    @Test
    fun testResetSettings() {}

    @Test
    fun testSetArpValue() {}

    @Test
    fun testSetDefaults() {}

    @Test
    fun testTestSetDefaults() {}

    @Test
    fun testSetGuidAndInit() {}

    @Test
    fun testSetMinimumFetchIntervalInSeconds() {}

    @Test
    fun testCanRequest() {}
}