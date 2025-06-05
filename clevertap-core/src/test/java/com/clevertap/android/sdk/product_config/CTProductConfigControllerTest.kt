package com.clevertap.android.sdk.product_config

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
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
class CTProductConfigControllerTest : BaseTestCase() {

    private lateinit var mProductConfigController: CTProductConfigController
    private lateinit var coreMetaData: CoreMetaData
    private lateinit var analyticsManager: BaseAnalyticsManager
    private lateinit var callbackManager: BaseCallbackManager
    private lateinit var productConfigSettings: ProductConfigSettings
    private lateinit var deviceInfo: DeviceInfo
    private lateinit var fileUtils: FileUtils
    private lateinit var listener: CTProductConfigListener
    private lateinit var guid: String

    override fun setUp() {
        super.setUp()
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            guid = "1212121221"
            coreMetaData = CoreMetaData()
            analyticsManager = mockk(relaxed = true)
            deviceInfo = MockDeviceInfo(application, cleverTapInstanceConfig, guid, coreMetaData)
            callbackManager = CallbackManager(cleverTapInstanceConfig, deviceInfo)
            listener = mockk(relaxed = true)
            callbackManager.productConfigListener = listener
            productConfigSettings = mockk(relaxed = true)
            every { productConfigSettings.guid } returns guid
            fileUtils = spyk(FileUtils(application, cleverTapInstanceConfig))
            mProductConfigController = CTProductConfigController(
                application,
                cleverTapInstanceConfig,
                analyticsManager,
                coreMetaData,
                callbackManager,
                productConfigSettings, fileUtils
            )
        }

        mProductConfigController.setDefaults(MockPCResponse().getDefaultConfig())
    }

    @Test
    fun testInit() {
        Assert.assertTrue(mProductConfigController.isInitialized.get())
    }

    @Test
    fun testFetch_Valid_Guid_Window_Expired() {
        val windowInSeconds = TimeUnit.MINUTES.toSeconds(12)
        every { productConfigSettings.nextFetchIntervalInSeconds } returns windowInSeconds
        val lastResponseTime =
            System.currentTimeMillis() - 2 * windowInSeconds * TimeUnit.SECONDS.toMillis(1)
        every { productConfigSettings.lastFetchTimeStampInMillis } returns lastResponseTime

        mProductConfigController.fetch()
        verify { analyticsManager.sendFetchEvent(any()) }
        Assert.assertTrue(coreMetaData.isProductConfigRequested)
    }

    @Test
    fun testFetch_Valid_Guid_Window_Not_Expired_Request_Not_Sent() {
        val windowInSeconds = TimeUnit.MINUTES.toSeconds(12)
        every { productConfigSettings.nextFetchIntervalInSeconds } returns windowInSeconds

        val lastResponseTime =
            System.currentTimeMillis() - windowInSeconds / 2 * TimeUnit.SECONDS.toMillis(1)
        every { productConfigSettings.lastFetchTimeStampInMillis } returns lastResponseTime

        mProductConfigController.fetch()

        verify(exactly = 0) { analyticsManager.sendFetchEvent(any()) }
        Assert.assertFalse(coreMetaData.isProductConfigRequested)
    }

    @Test
    fun testFetch_InValid_Guid_Request_Not_Sent() {
        val windowInSeconds = TimeUnit.MINUTES.toSeconds(12)
        every { productConfigSettings.nextFetchIntervalInSeconds } returns windowInSeconds
        val lastResponseTime = System.currentTimeMillis() - (windowInSeconds * 1000 * 2)
        every { productConfigSettings.lastFetchTimeStampInMillis } returns lastResponseTime
        every { productConfigSettings.guid } returns ""
        mProductConfigController.fetch()
        verify(exactly = 0) { analyticsManager.sendFetchEvent(any()) }
        Assert.assertFalse(coreMetaData.isProductConfigRequested)
    }

    @Test
    fun testReset_Settings() {
        mProductConfigController.resetSettings()
        verify { productConfigSettings.reset(any()) }
    }

    @Test
    fun test_Reset() {
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            mProductConfigController.reset()
            Assert.assertEquals(0, mProductConfigController.defaultConfigs.size)
            Assert.assertEquals(0, mProductConfigController.activatedConfigs.size)
            verify { productConfigSettings.initDefaults() }
            val productConfigDirName = mProductConfigController.productConfigDirName
            verify { fileUtils.deleteDirectory(productConfigDirName) }
        }
    }

    @Test
    fun test_setArpValue() {
        val jsonObject = JSONObject()
        mProductConfigController.setArpValue(jsonObject)
        verify { productConfigSettings.setARPValue(jsonObject) }
    }

    @Test
    fun test_setMinimumFetchIntervalInSeconds() {
        val timeInSec = TimeUnit.MINUTES.toSeconds(5)
        mProductConfigController.setMinimumFetchIntervalInSeconds(timeInSec)
        verify { productConfigSettings.setMinimumFetchIntervalInSeconds(timeInSec) }
    }

    @Test
    fun test_Getters() {
        Assert.assertEquals(mProductConfigController.analyticsManager, analyticsManager)
        Assert.assertEquals(mProductConfigController.callbackManager, callbackManager)
        Assert.assertEquals(mProductConfigController.config, cleverTapInstanceConfig)
        Assert.assertEquals(mProductConfigController.coreMetaData, coreMetaData)
        Assert.assertEquals(mProductConfigController.settings, productConfigSettings)
    }

    @Test
    fun test_activate() {
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            val activatedPath = mProductConfigController.activatedFullPath
            every { fileUtils.readFromFile(activatedPath) } returns
                    JSONObject(MockPCResponse().getFetchedConfig()).toString()
            mProductConfigController.activate()
            verify { listener.onActivated() }
            Assert.assertEquals(333333L, mProductConfigController.getLong("fetched_long"))
            Assert.assertEquals(
                "This is fetched string",
                mProductConfigController.getString("fetched_str")
            )
            Assert.assertEquals(
                44444.4444,
                mProductConfigController.getDouble("fetched_double"),
                0.1212
            )
            Assert.assertEquals(true, mProductConfigController.getBoolean("fetched_bool"))
            Assert.assertEquals("This is def_string", mProductConfigController.getString("def_str"))
            Assert.assertEquals(11111L, mProductConfigController.getLong("def_long"))
            Assert.assertEquals(2222.2222, mProductConfigController.getDouble("def_double"), 0.1212)
            Assert.assertEquals(false, mProductConfigController.getBoolean("def_bool"))
        }
    }

    @Test
    fun test_activate_Fail() {
        mockkStatic(CTExecutorFactory::class) {
            every { productConfigSettings.guid } returns ""
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            mProductConfigController.activate()
            verify(exactly = 0) { listener.onActivated() }
        }
    }

    @Test
    fun test_fetch_activate() {
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            val windowInSeconds = TimeUnit.MINUTES.toSeconds(12)
            every { productConfigSettings.nextFetchIntervalInSeconds } returns windowInSeconds
            val lastResponseTime =
                System.currentTimeMillis() - 2 * windowInSeconds * TimeUnit.SECONDS.toMillis(1)
            every { productConfigSettings.lastFetchTimeStampInMillis } returns lastResponseTime

            mProductConfigController.fetchAndActivate()
            verify { analyticsManager.sendFetchEvent(any()) }
            Assert.assertTrue(coreMetaData.isProductConfigRequested)

            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            val activatedPath = mProductConfigController.activatedFullPath
            every { fileUtils.readFromFile(activatedPath) } returns
                    JSONObject(MockPCResponse().getFetchedConfig()).toString()

            mProductConfigController.activate()
            verify { listener.onActivated() }
            Assert.assertEquals(
                "This is fetched string",
                mProductConfigController.getString("fetched_str")
            )
            Assert.assertEquals(333333L, mProductConfigController.getLong("fetched_long"))
            Assert.assertEquals(
                44444.4444,
                mProductConfigController.getDouble("fetched_double"),
                0.1212
            )
            Assert.assertEquals(true, mProductConfigController.getBoolean("fetched_bool"))
            Assert.assertEquals("This is def_string", mProductConfigController.getString("def_str"))
            Assert.assertEquals(11111L, mProductConfigController.getLong("def_long"))
            Assert.assertEquals(2222.2222, mProductConfigController.getDouble("def_double"), 0.1212)
            Assert.assertEquals(false, mProductConfigController.getBoolean("def_bool"))
        }
    }

    @Test
    fun test_getLastFetchTimeStampInMillis() {
        mProductConfigController.lastFetchTimeStampInMillis
        verify { productConfigSettings.lastFetchTimeStampInMillis }
    }

    @Test
    fun test_onFetchSuccess() {
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            mProductConfigController.onFetchSuccess(MockPCResponse().getMockPCResponse())
            val dirName = mProductConfigController.productConfigDirName
            verify {
                fileUtils.writeJsonToFile(
                    dirName,
                    CTProductConfigConstants.FILE_NAME_ACTIVATED,
                    any()
                )
            }
            verify { listener.onFetched() }
        }
    }

    @Test
    fun test_onFetchFailed() {
        mProductConfigController.onFetchFailed()
        Assert.assertEquals(mProductConfigController.isFetchAndActivating, false)
    }

    @Test
    fun test_setGuidAndInit_whenCleverTapIDNull_GuidNotSet() {
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            val controller = spyk(mProductConfigController)
            val newGuid = ""
            controller.setGuidAndInit(newGuid)
            Assert.assertEquals(controller.settings.guid, guid)
            verify(exactly = 0) { productConfigSettings.guid = newGuid }
            verify(exactly = 0) { controller.initAsync() }
        }
    }

    @Test
    fun test_setGuidAndInit_tryingToSetGuidWhenAlreadyInitialised_ShouldNotUpdateGuid() {
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            val controller = spyk(mProductConfigController)
            controller.isInitialized = AtomicBoolean(true)
            val newGuid = "333333333"
            controller.setGuidAndInit(newGuid)
            Assert.assertEquals(controller.settings.guid, guid)
            verify(exactly = 0) { productConfigSettings.guid = newGuid }
            verify(exactly = 0) { controller.initAsync() }
        }
    }

    @Test
    fun test_setGuidAndInit_tryingToSetGuidWhenNotInitialised_GuidIsUpdatedWithInitialisation() {
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            val settings = ProductConfigSettings(guid, cleverTapInstanceConfig, fileUtils)
            val controller = spyk(
                CTProductConfigController(
                    application,
                    cleverTapInstanceConfig,
                    analyticsManager,
                    coreMetaData,
                    callbackManager,
                    settings, fileUtils
                )
            )
            controller.isInitialized.set(false)
            val newGuid = "333333333"
            controller.setGuidAndInit(newGuid)
            Assert.assertEquals(controller.settings.guid, newGuid)
            verify { controller.initAsync() }
            Assert.assertTrue(controller.isInitialized.get())
        }
    }

    @Test
    fun test_setDefaultsWithXmlParser() {
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            val resId = 12212
            val xmlParser = mockk<DefaultXmlParser>(relaxed = true)
            val mockPCResponse = MockPCResponse()
            every {
                xmlParser.getDefaultsFromXml(
                    application, resId
                )
            } returns mockPCResponse.getDefaultConfig() as HashMap<String, String>
            val controller = spyk(mProductConfigController)
            controller.setDefaultsWithXmlParser(resId, xmlParser)
            verify { controller.initAsync() }
            Assert.assertEquals("This is def_string", controller.getString("def_str"))
            Assert.assertEquals("11111", controller.getString("def_long"))
            Assert.assertEquals("2222.2222", controller.getString("def_double"))
            Assert.assertEquals("false", controller.getString("def_bool"))
        }
    }

    @Test
    fun test_setDefaultUsingXML() {
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            val controller = spyk(mProductConfigController)
            val resId = 12212
            controller.setDefaults(resId)
            verify { controller.setDefaultsWithXmlParser(eq(resId), any()) }
        }
    }
}
