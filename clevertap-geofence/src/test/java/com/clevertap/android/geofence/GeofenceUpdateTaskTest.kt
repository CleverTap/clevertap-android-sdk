package com.clevertap.android.geofence

import android.content.Context
import com.clevertap.android.geofence.fakes.GeofenceJSON
import com.clevertap.android.geofence.interfaces.CTGeofenceAdapter
import com.clevertap.android.geofence.model.CTGeofence
import com.google.android.gms.tasks.OnSuccessListener
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.skyscreamer.jsonassert.JSONAssert

class GeofenceUpdateTaskTest : BaseTestCase() {

    @Mock
    lateinit var ctGeofenceAPI: CTGeofenceAPI

    @Mock
    lateinit var ctGeofenceAdapter: CTGeofenceAdapter

    private lateinit var ctGeofenceAPIMockedStatic: MockedStatic<CTGeofenceAPI>

    private lateinit var fileUtilsMockedStatic: MockedStatic<FileUtils>

    @Mock
    lateinit var logger: Logger

    @Before
    override fun setUp() {
        MockitoAnnotations.openMocks(this)
        super.setUp()
        ctGeofenceAPIMockedStatic = Mockito.mockStatic(CTGeofenceAPI::class.java)
        fileUtilsMockedStatic = Mockito.mockStatic(FileUtils::class.java)
        `when`(CTGeofenceAPI.getInstance(application)).thenReturn(ctGeofenceAPI)
        `when`(CTGeofenceAPI.getLogger()).thenReturn(logger)
        `when`(ctGeofenceAPI.ctGeofenceAdapter).thenReturn(ctGeofenceAdapter)
    }

    @After
    fun cleanup() {
        ctGeofenceAPIMockedStatic.close()
        fileUtilsMockedStatic.close()
    }

    @Test
    fun executeTestTC1() {

        // when old geofence is empty and geofence monitor count is less than new geofence list size

        `when`(FileUtils.getCachedDirName(application)).thenReturn("")
        `when`(FileUtils.getCachedFullPath(any(Context::class.java), anyString())).thenReturn("")
        `when`(
            FileUtils.readFromFile(
                any(
                    Context::class.java
                ), anyString()
            )
        ).thenReturn("")

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().setGeofenceMonitoringCount(1).build()

        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(currentGeofenceSettings)

        val updateTask = GeofenceUpdateTask(
            application, GeofenceJSON.geofence
        )

        updateTask.execute()

        val argumentCaptorJson = ArgumentCaptor.forClass(JSONObject::class.java)

        fileUtilsMockedStatic.verify {
            FileUtils.writeJsonToFile(
                any(Context::class.java), anyString(), anyString(), argumentCaptorJson.capture()
            )
        }

        JSONAssert.assertEquals(GeofenceJSON.first, argumentCaptorJson.getValue(), true)

        val argumentCaptor =
            ArgumentCaptor.forClass(MutableList::class.java) as ArgumentCaptor<MutableList<CTGeofence>>

        verify(ctGeofenceAdapter).addAllGeofence(
            argumentCaptor.capture(), any(OnSuccessListener::class.java)
        )
        assertEquals(1, argumentCaptor.getValue().size)
    }

    @Test
    fun executeTestTC2() {
        // when old geofence is empty and geofence monitor count is greater than new geofence list size

        `when`(FileUtils.getCachedDirName(application)).thenReturn("")
        `when`(FileUtils.getCachedFullPath(any(Context::class.java), anyString())).thenReturn("")
        `when`(
            FileUtils.readFromFile(
                any(
                    Context::class.java
                ), anyString()
            )
        ).thenReturn("")

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().setGeofenceMonitoringCount(1).build()

        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(currentGeofenceSettings)

        val updateTask = GeofenceUpdateTask(
            application, GeofenceJSON.emptyGeofence
        )

        updateTask.execute()

        val argumentCaptorJson = ArgumentCaptor.forClass(JSONObject::class.java)

        fileUtilsMockedStatic.verify {
            FileUtils.writeJsonToFile(
                any(Context::class.java), anyString(), anyString(), argumentCaptorJson.capture()
            )
        }

        JSONAssert.assertEquals(
            GeofenceJSON.emptyGeofence, argumentCaptorJson.getValue(), true
        )

        val argumentCaptor =
            ArgumentCaptor.forClass(MutableList::class.java) as ArgumentCaptor<MutableList<CTGeofence>>

        verify(ctGeofenceAdapter).addAllGeofence(
            argumentCaptor.capture(), any(OnSuccessListener::class.java)
        )
        assertEquals(argumentCaptor.getValue().size, 0)
    }

    @Test
    fun executeTestTC3() {
        // when old geofence is empty and new geofence json is invalid

        `when`(FileUtils.getCachedDirName(application)).thenReturn("")
        `when`(FileUtils.getCachedFullPath(any(Context::class.java), anyString())).thenReturn("")
        `when`(
            FileUtils.readFromFile(
                any(
                    Context::class.java
                ), anyString()
            )
        ).thenReturn("")

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().setGeofenceMonitoringCount(1).build()

        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(currentGeofenceSettings)

        val updateTask = GeofenceUpdateTask(application, GeofenceJSON.emptyJson)

        updateTask.execute()

        val argumentCaptorJson = ArgumentCaptor.forClass(JSONObject::class.java)

        fileUtilsMockedStatic.verify {
            FileUtils.writeJsonToFile(
                any(Context::class.java), anyString(), anyString(), argumentCaptorJson.capture()
            )
        }

        JSONAssert.assertEquals(GeofenceJSON.emptyJson, argumentCaptorJson.getValue(), true)

        val argumentCaptor =
            ArgumentCaptor.forClass(MutableList::class.java) as ArgumentCaptor<MutableList<CTGeofence>>

        verify(ctGeofenceAdapter).addAllGeofence(
            argumentCaptor.capture(), any(OnSuccessListener::class.java)
        )
        assertEquals(argumentCaptor.getValue().size, 0)
    }

    @Test
    fun executeTestTC4() {
        // when old geofence is not empty and new geofence list is not empty

        `when`(FileUtils.getCachedDirName(application)).thenReturn("")
        `when`(FileUtils.getCachedFullPath(any(Context::class.java), anyString())).thenReturn("")
        `when`(
            FileUtils.readFromFile(
                any(
                    Context::class.java
                ), anyString()
            )
        ).thenReturn(GeofenceJSON.first.toString())

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().setGeofenceMonitoringCount(1).build()

        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(currentGeofenceSettings)

        val updateTask = GeofenceUpdateTask(
            application, GeofenceJSON.geofence
        )

        updateTask.execute()

        val argumentCaptorOldGeofence =
            ArgumentCaptor.forClass(MutableList::class.java) as ArgumentCaptor<MutableList<String>>

        verify(ctGeofenceAdapter).removeAllGeofence(
            argumentCaptorOldGeofence.capture(), any(OnSuccessListener::class.java)
        )
        assertEquals(listOf("310001"), argumentCaptorOldGeofence.getValue())

    }

    @Test
    fun executeTestTC5() {
        // when old geofence is not empty and new geofence list is null

        `when`(FileUtils.getCachedDirName(application)).thenReturn("")
        `when`(FileUtils.getCachedFullPath(any(Context::class.java), anyString())).thenReturn("")
        `when`(
            FileUtils.readFromFile(
                any(
                    Context::class.java
                ), anyString()
            )
        ).thenReturn(GeofenceJSON.GEOFENCE_JSON_STRING)

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().setGeofenceMonitoringCount(2).build()

        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(currentGeofenceSettings)

        val updateTask = GeofenceUpdateTask(application, null)

        updateTask.execute()

        val argumentCaptorJson = ArgumentCaptor.forClass(JSONObject::class.java)

        fileUtilsMockedStatic.verify {
            FileUtils.writeJsonToFile(
                any(Context::class.java), anyString(), anyString(), argumentCaptorJson.capture()
            )
        }
        JSONAssert.assertEquals(GeofenceJSON.geofence, argumentCaptorJson.getValue(), true)

        val argumentCaptor =
            ArgumentCaptor.forClass(MutableList::class.java) as ArgumentCaptor<MutableList<CTGeofence>>

        verify(ctGeofenceAdapter).addAllGeofence(
            argumentCaptor.capture(), any(OnSuccessListener::class.java)
        )
        assertEquals(argumentCaptor.getValue().size, 2)
    }
}
