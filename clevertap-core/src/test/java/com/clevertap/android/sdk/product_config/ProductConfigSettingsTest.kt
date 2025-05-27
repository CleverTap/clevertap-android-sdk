package com.clevertap.android.sdk.product_config

import com.clevertap.android.sdk.product_config.CTProductConfigConstants.DEFAULT_MIN_FETCH_INTERVAL_SECONDS
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
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
internal class ProductConfigSettingsTest : BaseTestCase() {

    lateinit var settings: ProductConfigSettings
    val guid = "1212121"
    lateinit var fileUtils: FileUtils

    @Before
    override fun setUp() {
        super.setUp()
        fileUtils = mockk(relaxed = true)
        settings = ProductConfigSettings(guid, cleverTapInstanceConfig, fileUtils)
    }

    @Test
    fun aInitTest() {
        Assert.assertEquals(settings.lastFetchTimeStampInMillis, 0)
        Assert.assertEquals(settings.nextFetchIntervalInSeconds, DEFAULT_MIN_FETCH_INTERVAL_SECONDS)
    }

    @Test
    fun testTimestamp() {
        val timeMillis = System.currentTimeMillis()
        settings.lastFetchTimeStampInMillis = timeMillis
        Assert.assertEquals(timeMillis, settings.lastFetchTimeStampInMillis)
    }

    @Test
    fun test_set_guid() {
        val guid = "121212"
        settings.guid = guid
        Assert.assertEquals(guid, settings.guid)
    }

    @Test
    fun test_Set_ARP_SDK_Time_Not_Set() {
        //***************     Client has not set the fetch interval from SDK side
        // case 1: Server Settings is greater than default values
        var jsonObject = JSONObject()
        jsonObject.put(CTProductConfigConstants.PRODUCT_CONFIG_NO_OF_CALLS, 2)
        jsonObject.put(CTProductConfigConstants.PRODUCT_CONFIG_WINDOW_LENGTH_MINS, 60)
        settings.setARPValue(jsonObject)

        Assert.assertEquals(settings.nextFetchIntervalInSeconds, TimeUnit.MINUTES.toSeconds(60 / 2))

        // case 2: Server Settings is smaller than default values
        jsonObject = JSONObject()
        jsonObject.put(CTProductConfigConstants.PRODUCT_CONFIG_NO_OF_CALLS, 10)
        jsonObject.put(CTProductConfigConstants.PRODUCT_CONFIG_WINDOW_LENGTH_MINS, 60)
        settings.setARPValue(jsonObject)

        Assert.assertEquals(settings.nextFetchIntervalInSeconds, DEFAULT_MIN_FETCH_INTERVAL_SECONDS)
    }

    @Test
    fun test_Set_ARP_SDK_Time_Set() {
        //***************     Client has set the fetch interval from SDK side
        // case 1: Server Settings is greater than SDK set value
        settings.setMinimumFetchIntervalInSeconds(TimeUnit.MINUTES.toSeconds(17))
        var jsonObject = JSONObject()
        jsonObject.put(CTProductConfigConstants.PRODUCT_CONFIG_NO_OF_CALLS, 2)
        jsonObject.put(CTProductConfigConstants.PRODUCT_CONFIG_WINDOW_LENGTH_MINS, 60)
        settings.setARPValue(jsonObject)

        Assert.assertEquals(settings.nextFetchIntervalInSeconds, TimeUnit.MINUTES.toSeconds(60 / 2))


        settings.setMinimumFetchIntervalInSeconds(TimeUnit.MINUTES.toSeconds(45))
        // case 2: Server Settings is smaller than default values
        jsonObject = JSONObject()
        jsonObject.put(CTProductConfigConstants.PRODUCT_CONFIG_NO_OF_CALLS, 10)
        jsonObject.put(CTProductConfigConstants.PRODUCT_CONFIG_WINDOW_LENGTH_MINS, 50)
        settings.setARPValue(jsonObject)

        Assert.assertEquals(settings.nextFetchIntervalInSeconds, TimeUnit.MINUTES.toSeconds(45))
    }

    @Test
    fun testReset() {
        mockkStatic(CTExecutorFactory::class) {
            every { CTExecutorFactory.executors(cleverTapInstanceConfig) } returns MockCTExecutors(
                cleverTapInstanceConfig
            )
            settings.reset(fileUtils)
            verify { fileUtils.deleteFile(settings.fullPath) }
        }
        aInitTest()
    }

    @Test
    fun testLoadSettings_Empty_String() {
        settings.loadSettings(fileUtils)
        every { fileUtils.readFromFile(any()) } returns ""
        verify { fileUtils.readFromFile(settings.fullPath) }
        aInitTest()
    }

    @Test(expected = IllegalArgumentException::class)
    fun when_Null_fileUtils_then_loadSetting_throws_Exception() {
        settings.loadSettings(null)
    }

    @Test
    fun when_fileUtils_Throws_Exception_loadSetting_throws_Exception() {
        val spySettings = spyk(settings)
        every { fileUtils.readFromFile(settings.fullPath) } throws RuntimeException("Something went wrong")
        spySettings.loadSettings(fileUtils)
        verify(exactly = 0) { spySettings.getJsonObject(any()) }
        verify(exactly = 0) { spySettings.populateMapWithJson(any()) }
    }

    @Test
    fun testLoadSettings_Non_Empty_Data() {
        val map = HashMap<String, String>()
        map.put(CTProductConfigConstants.PRODUCT_CONFIG_NO_OF_CALLS, "10")
        map.put(CTProductConfigConstants.PRODUCT_CONFIG_WINDOW_LENGTH_MINS, "60")
        val timeStamp = System.currentTimeMillis()
        map.put(CTProductConfigConstants.KEY_LAST_FETCHED_TIMESTAMP, timeStamp.toString())
        map.put(
            CTProductConfigConstants.PRODUCT_CONFIG_MIN_INTERVAL_IN_SECONDS,
            TimeUnit.MINUTES.toSeconds(16).toString()
        )
        val jsonString = JSONObject(map as Map<*, *>).toString()
        every { fileUtils.readFromFile(any()) } returns jsonString

        settings.loadSettings(fileUtils)
        settings.setMinimumFetchIntervalInSeconds(TimeUnit.MINUTES.toSeconds(45))

        verify { fileUtils.readFromFile(settings.fullPath) }
        Assert.assertEquals(settings.lastFetchTimeStampInMillis, timeStamp)
        Assert.assertEquals(settings.nextFetchIntervalInSeconds, TimeUnit.MINUTES.toSeconds(45))


        settings.loadSettings(fileUtils)
        settings.setMinimumFetchIntervalInSeconds(TimeUnit.MINUTES.toSeconds(5))

        Assert.assertEquals(settings.lastFetchTimeStampInMillis, timeStamp)
        Assert.assertEquals(settings.nextFetchIntervalInSeconds, TimeUnit.MINUTES.toSeconds(6))
    }
}
