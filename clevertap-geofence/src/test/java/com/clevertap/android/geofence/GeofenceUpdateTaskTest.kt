package com.clevertap.android.geofence

import com.clevertap.android.geofence.fakes.GeofenceJSON
import com.clevertap.android.geofence.interfaces.CTGeofenceAdapter
import com.clevertap.android.geofence.model.CTGeofence
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

class GeofenceUpdateTaskTest : BaseTestCase() {

    private lateinit var ctGeofenceAPI: CTGeofenceAPI
    private lateinit var ctGeofenceAdapter: CTGeofenceAdapter
    private lateinit var logger: Logger

    @Before
    override fun setUp() {
        super.setUp()
        ctGeofenceAPI = mockk(relaxed = true)
        ctGeofenceAdapter = mockk(relaxed = true)
        logger = mockk(relaxed = true)

        mockkStatic(CTGeofenceAPI::class)
        mockkStatic(FileUtils::class)

        every { CTGeofenceAPI.getInstance(application) } returns ctGeofenceAPI
        every { CTGeofenceAPI.getLogger() } returns logger
        every { ctGeofenceAPI.ctGeofenceAdapter } returns ctGeofenceAdapter
    }

    @After
    override fun cleanUp() {
        super.cleanUp()
        unmockkStatic(CTGeofenceAPI::class)
        unmockkStatic(FileUtils::class)
    }

    @Test
    fun executeTestTC1() {
        // when old geofence is empty and geofence monitor count is less than new geofence list size

        every { FileUtils.getCachedDirName(application) } returns ""
        every { FileUtils.getCachedFullPath(any(), any()) } returns ""
        every { FileUtils.readFromFile(any(), any()) } returns ""

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().setGeofenceMonitoringCount(1).build()
        every { ctGeofenceAPI.geofenceSettings } returns currentGeofenceSettings

        val updateTask = GeofenceUpdateTask(application, GeofenceJSON.geofence)
        updateTask.execute()

        val jsonSlot = slot<JSONObject>()
        verify { FileUtils.writeJsonToFile(any(), any(), any(), capture(jsonSlot)) }

        JSONAssert.assertEquals(GeofenceJSON.first, jsonSlot.captured, true)

        val geofenceListSlot = slot<MutableList<CTGeofence>>()
        verify { ctGeofenceAdapter.addAllGeofence(capture(geofenceListSlot), any()) }

        assertEquals(1, geofenceListSlot.captured.size)
    }

    @Test
    fun executeTestTC2() {
        // when old geofence is empty and geofence monitor count is greater than new geofence list size

        every { FileUtils.getCachedDirName(application) } returns ""
        every { FileUtils.getCachedFullPath(any(), any()) } returns ""
        every { FileUtils.readFromFile(any(), any()) } returns ""

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().setGeofenceMonitoringCount(1).build()
        every { ctGeofenceAPI.geofenceSettings } returns currentGeofenceSettings

        val updateTask = GeofenceUpdateTask(application, GeofenceJSON.emptyGeofence)
        updateTask.execute()

        val jsonSlot = slot<JSONObject>()
        verify { FileUtils.writeJsonToFile(any(), any(), any(), capture(jsonSlot)) }

        JSONAssert.assertEquals(GeofenceJSON.emptyGeofence, jsonSlot.captured, true)

        val geofenceListSlot = slot<MutableList<CTGeofence>>()
        verify { ctGeofenceAdapter.addAllGeofence(capture(geofenceListSlot), any()) }

        assertEquals(0, geofenceListSlot.captured.size)
    }

    @Test
    fun executeTestTC3() {
        // when old geofence is empty and new geofence json is invalid

        every { FileUtils.getCachedDirName(application) } returns ""
        every { FileUtils.getCachedFullPath(any(), any()) } returns ""
        every { FileUtils.readFromFile(any(), any()) } returns ""

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().setGeofenceMonitoringCount(1).build()
        every { ctGeofenceAPI.geofenceSettings } returns currentGeofenceSettings

        val updateTask = GeofenceUpdateTask(application, GeofenceJSON.emptyJson)
        updateTask.execute()

        val jsonSlot = slot<JSONObject>()
        verify { FileUtils.writeJsonToFile(any(), any(), any(), capture(jsonSlot)) }

        JSONAssert.assertEquals(GeofenceJSON.emptyJson, jsonSlot.captured, true)

        val geofenceListSlot = slot<MutableList<CTGeofence>>()
        verify { ctGeofenceAdapter.addAllGeofence(capture(geofenceListSlot), any()) }

        assertEquals(0, geofenceListSlot.captured.size)
    }

    @Test
    fun executeTestTC4() {
        // when old geofence is not empty and new geofence list is not empty

        every { FileUtils.getCachedDirName(application) } returns ""
        every { FileUtils.getCachedFullPath(any(), any()) } returns ""
        every { FileUtils.readFromFile(any(), any()) } returns GeofenceJSON.first.toString()

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().setGeofenceMonitoringCount(1).build()
        every { ctGeofenceAPI.geofenceSettings } returns currentGeofenceSettings

        val updateTask = GeofenceUpdateTask(application, GeofenceJSON.geofence)
        updateTask.execute()

        val idListSlot = slot<MutableList<String>>()
        verify { ctGeofenceAdapter.removeAllGeofence(capture(idListSlot), any()) }

        assertEquals(listOf("310001"), idListSlot.captured)
    }

    @Test
    fun executeTestTC5() {
        // when old geofence is not empty and new geofence list is null

        every { FileUtils.getCachedDirName(application) } returns ""
        every { FileUtils.getCachedFullPath(any(), any()) } returns ""
        every { FileUtils.readFromFile(any(), any()) } returns GeofenceJSON.GEOFENCE_JSON_STRING

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().setGeofenceMonitoringCount(2).build()
        every { ctGeofenceAPI.geofenceSettings } returns currentGeofenceSettings

        val updateTask = GeofenceUpdateTask(application, null)
        updateTask.execute()

        val jsonSlot = slot<JSONObject>()
        verify { FileUtils.writeJsonToFile(any(), any(), any(), capture(jsonSlot)) }

        JSONAssert.assertEquals(GeofenceJSON.geofence, jsonSlot.captured, true)

        val geofenceListSlot = slot<MutableList<CTGeofence>>()
        verify { ctGeofenceAdapter.addAllGeofence(capture(geofenceListSlot), any()) }

        assertEquals(2, geofenceListSlot.captured.size)
    }
}
