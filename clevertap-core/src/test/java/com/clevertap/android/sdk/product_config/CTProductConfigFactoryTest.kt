package com.clevertap.android.sdk.product_config

import com.clevertap.android.sdk.BaseAnalyticsManager
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CallbackManager
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.MockAnalyticsManager
import com.clevertap.android.sdk.MockDeviceInfo
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CTProductConfigFactoryTest : BaseTestCase() {

    val guid = "1212212"
    lateinit var coreMetaData: CoreMetaData
    lateinit var deviceInfo: DeviceInfo
    lateinit var analyticsManager: BaseAnalyticsManager
    lateinit var callbackManager: BaseCallbackManager

    override fun setUp() {
        super.setUp()
        coreMetaData = CoreMetaData()
        deviceInfo = MockDeviceInfo(application, cleverTapInstanceConfig, guid, coreMetaData)
        analyticsManager = MockAnalyticsManager()
        callbackManager = CallbackManager(cleverTapInstanceConfig, deviceInfo)
    }

    @Test
    fun test_getInstance_not_null() {
        val controller = CTProductConfigFactory.getInstance(
            application,
            deviceInfo,
            cleverTapInstanceConfig,
            analyticsManager,
            coreMetaData,
            callbackManager
        )
        Assert.assertNotNull(controller)
        Assert.assertTrue(controller is CTProductConfigController)
    }

    @Test
    fun test_getInstance_config_correct() {
        val controller = CTProductConfigFactory.getInstance(
            application,
            deviceInfo,
            cleverTapInstanceConfig,
            analyticsManager,
            coreMetaData,
            callbackManager
        )
        Assert.assertNotNull(controller.fileUtils)
        Assert.assertEquals(controller.settings.guid, guid)
        Assert.assertEquals(controller.config, cleverTapInstanceConfig)
        Assert.assertEquals(controller.coreMetaData, coreMetaData)
        Assert.assertEquals(controller.callbackManager, callbackManager)
        Assert.assertEquals(controller.analyticsManager, analyticsManager)
    }
}