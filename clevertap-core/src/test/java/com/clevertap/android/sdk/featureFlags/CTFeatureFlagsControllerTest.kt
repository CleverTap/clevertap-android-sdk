package com.clevertap.android.sdk.featureFlags

import com.clevertap.android.sdk.BaseAnalyticsManager
import com.clevertap.android.sdk.BaseCallbackManager
import com.clevertap.android.sdk.CTFeatureFlagsListener
import com.clevertap.android.sdk.CallbackManager
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.MockDeviceInfo
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.MockCTExecutors
import com.clevertap.android.sdk.utils.FileUtils
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CTFeatureFlagsControllerTest : BaseTestCase() {

    private lateinit var mCTFeatureFlagsController: CTFeatureFlagsController
    private lateinit var coreMetaData: CoreMetaData
    private lateinit var analyticsManager: BaseAnalyticsManager
    private lateinit var callbackManager: BaseCallbackManager
    private lateinit var deviceInfo: DeviceInfo
    private lateinit var fileUtils: FileUtils
    private lateinit var featureFlagsListener: CTFeatureFlagsListener
    private val guid = "1212121212"

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            coreMetaData = CoreMetaData()
            analyticsManager = mockk(relaxed = true)
            deviceInfo = MockDeviceInfo(application, cleverTapInstanceConfig, guid, coreMetaData)
            callbackManager = CallbackManager(cleverTapInstanceConfig, deviceInfo)
            fileUtils = spyk(FileUtils(application, cleverTapInstanceConfig))
            featureFlagsListener = mockk(relaxed = true)
            callbackManager.featureFlagListener = featureFlagsListener
            mCTFeatureFlagsController = CTFeatureFlagsController(
                guid,
                cleverTapInstanceConfig,
                callbackManager,
                analyticsManager, fileUtils
            )
        }
    }

    @Test
    fun test_constructor_whenFeatureFlagIsNotSave_InitShouldReturnTrue() {
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            val controller = spyk(mCTFeatureFlagsController)
            Assert.assertTrue(controller.isInitialized)
        }
    }

    @Test
    fun when_Non_Empty_Feature_Flag_Config_Then_Init_Return_True() {
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            every { fileUtils.readFromFile(mCTFeatureFlagsController.getCachedFullPath()) } returns
                    MockFFResponse().getResponseJSON().toString()

            val controller = CTFeatureFlagsController(
                guid,
                cleverTapInstanceConfig,
                callbackManager,
                analyticsManager, fileUtils
            )
            Assert.assertTrue(controller.isInitialized)
        }
    }

    @Test
    fun when_Non_Empty_Feature_Flag_Config_Read_Exception_Then_Init_Return_True() {
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            every { fileUtils.readFromFile(mCTFeatureFlagsController.getCachedFullPath()) } throws
                    RuntimeException("Something Went Wrong")

            val controller = CTFeatureFlagsController(
                guid,
                cleverTapInstanceConfig,
                callbackManager,
                analyticsManager, fileUtils
            )
            Assert.assertFalse(controller.isInitialized)
        }
    }

    @Test
    fun when_Feature_Flag_Response_Success() {
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            every { fileUtils.readFromFile(mCTFeatureFlagsController.getCachedFullPath()) } throws
                    RuntimeException("Something Went Wrong")

            val jsonObject = MockFFResponse().getResponseJSON()
            mCTFeatureFlagsController.updateFeatureFlags(jsonObject)
            verify { featureFlagsListener.featureFlagsUpdated() }
            verify {
                fileUtils.writeJsonToFile(
                    mCTFeatureFlagsController.cachedDirName,
                    mCTFeatureFlagsController.cachedFileName,
                    jsonObject
                )
            }

            Assert.assertTrue(mCTFeatureFlagsController.get("feature_A", false))
            Assert.assertFalse(mCTFeatureFlagsController.get("feature_B", true))
            Assert.assertFalse(mCTFeatureFlagsController.get("feature_C", true))
            Assert.assertTrue(mCTFeatureFlagsController.get("feature_D", false))
            Assert.assertTrue(mCTFeatureFlagsController.get("feature_E", true))
            Assert.assertFalse(mCTFeatureFlagsController.get("feature_F", false))
        }
    }

    @Test
    fun test_get_whenNotInitialized_shouldReturnDefaultValue() {
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            val jsonObject = MockFFResponse().getResponseJSON()
            mCTFeatureFlagsController.updateFeatureFlags(jsonObject)
            mCTFeatureFlagsController.isInitialized = false
            Assert.assertFalse(mCTFeatureFlagsController.get("feature_A", false))
        }
    }

    @Test
    fun test_Fetch_FF_Success() {
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            mCTFeatureFlagsController.fetchFeatureFlags()
            verify { analyticsManager.fetchFeatureFlags() }
        }
    }

    @Test
    fun test_resetWithGuid() {
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            val guid = "121u3203u129"
            val controller = spyk(mCTFeatureFlagsController)
            controller.resetWithGuid(guid)
            Assert.assertEquals(guid, controller.guid)
            verify { controller.init() }
        }
    }

    @Test
    fun test_setGuidAndInit_whenInitialised_initMethodShouldNotGetCalled() {
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            val controller = spyk(mCTFeatureFlagsController)
            val newGuid = "131"
            controller.setGuidAndInit(newGuid)
            Assert.assertNotEquals(controller.guid, newGuid)
            verify(exactly = 0) { controller.init() }
        }
    }

    @Test
    fun test_setGuidAndInit_whenNotInitialised_initMethodShouldGetCalled() {
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            val controller = spyk(mCTFeatureFlagsController)
            val newGuid = "131"
            controller.isInitialized = false
            controller.setGuidAndInit(newGuid)
            Assert.assertEquals(controller.guid, newGuid)
            verify { controller.init() }
        }
    }
}
