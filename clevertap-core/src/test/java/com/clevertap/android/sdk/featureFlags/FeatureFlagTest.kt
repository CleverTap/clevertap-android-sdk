package com.clevertap.android.sdk.featureFlags

import com.clevertap.android.sdk.BaseAnalyticsManager
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CallbackManager
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.MockDeviceInfo
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.utils.FileUtils
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FeatureFlagTest : BaseTestCase() {

    private lateinit var mCTFeatureFlagsController: CTFeatureFlagsController
    private lateinit var coreMetaData: CoreMetaData
    private lateinit var analyticsManager: BaseAnalyticsManager
    private lateinit var callbackManager: BaseCallbackManager
    private lateinit var deviceInfo: DeviceInfo
    private lateinit var fileUtils: FileUtils
    private val guid = "1212121212"

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        Mockito.mockStatic(CTExecutorFactory::class.java).use {
            Mockito.`when`(CTExecutorFactory.getInstance(cleverTapInstanceConfig)).thenReturn(MockCTExecutors())
            coreMetaData = CoreMetaData()
            analyticsManager = Mockito.mock(BaseAnalyticsManager::class.java)
            deviceInfo = MockDeviceInfo(application, cleverTapInstanceConfig, guid, coreMetaData)
            callbackManager = CallbackManager(cleverTapInstanceConfig, deviceInfo)
            fileUtils = Mockito.mock(FileUtils::class.java)
            fileUtils.setConfig(cleverTapInstanceConfig)
            fileUtils.setContext(application)
            mCTFeatureFlagsController = CTFeatureFlagsController(
                guid,
                cleverTapInstanceConfig,
                callbackManager,
                analyticsManager, fileUtils
            )
        }
    }

    @Test
    fun test_Fetch_Ff() {
        Mockito.mockStatic(CTExecutorFactory::class.java).use {
            Mockito.`when`(CTExecutorFactory.getInstance(cleverTapInstanceConfig)).thenReturn(MockCTExecutors())
            mCTFeatureFlagsController.fetchFeatureFlags()
            Mockito.verify(analyticsManager).fetchFeatureFlags()
        }
    }
}