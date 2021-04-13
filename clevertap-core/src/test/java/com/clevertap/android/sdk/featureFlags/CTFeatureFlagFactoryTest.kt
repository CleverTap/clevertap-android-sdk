package com.clevertap.android.sdk.featureFlags

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
internal class CTFeatureFlagFactoryTest : BaseTestCase() {

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
    fun test_getInstance_shouldNotReturnNull() {
        val controller = CTFeatureFlagsFactory.getInstance(
            application,
            guid,
            cleverTapInstanceConfig,
            callbackManager,
            analyticsManager
        )
        Assert.assertNotNull(controller)
        Assert.assertTrue(controller is CTFeatureFlagsController)
    }

    @Test
    fun test_getInstance_instanceInitializedProperly() {
        val controller = CTFeatureFlagsFactory.getInstance(
            application,
            guid,
            cleverTapInstanceConfig,
            callbackManager,
            analyticsManager
        )
        Assert.assertNotNull(controller.mFileUtils)
        Assert.assertEquals(controller.config, cleverTapInstanceConfig)
        Assert.assertEquals(controller.mCallbackManager, callbackManager)
        Assert.assertEquals(controller.mAnalyticsManager, analyticsManager)
    }
}