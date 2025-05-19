package com.clevertap.android.geofence

import android.app.PendingIntent
import com.clevertap.android.geofence.fakes.GeofenceJSON
import com.clevertap.android.geofence.interfaces.CTGeofenceAdapter
import com.clevertap.android.geofence.model.CTGeofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class GoogleGeofenceAdapterTest : BaseTestCase() {

    @Mock
    lateinit var ctGeofenceAPI: CTGeofenceAPI

    @Mock
    lateinit var geofencingClient: GeofencingClient

    @Mock
    lateinit var onSuccessListener: OnSuccessListener<Void>

    @Mock
    lateinit var pendingIntent: PendingIntent

    @Mock
    lateinit var task: Task<Void>

    lateinit var ctGeofenceAPIMockedStatic: MockedStatic<CTGeofenceAPI>

    @Mock
    lateinit var ctGeofenceAdapter: CTGeofenceAdapter

    private lateinit var locationServicesMockedStatic: MockedStatic<LocationServices>

    @Mock
    lateinit var logger: Logger

    private lateinit var tasksMockedStatic: MockedStatic<Tasks>

    private lateinit var utilsMockedStatic: MockedStatic<Utils>

    @After
    fun cleanup() {
        ctGeofenceAPIMockedStatic.close()
        locationServicesMockedStatic.close()
        utilsMockedStatic.close()
        tasksMockedStatic.close()
    }

    @Before
    override fun setUp() {
        MockitoAnnotations.openMocks(this)
        ctGeofenceAPIMockedStatic = mockStatic(CTGeofenceAPI::class.java)
        locationServicesMockedStatic = mockStatic(LocationServices::class.java)
        utilsMockedStatic = mockStatic(Utils::class.java)
        tasksMockedStatic = mockStatic(Tasks::class.java)

        super.setUp()

        `when`(CTGeofenceAPI.getInstance(application)).thenReturn(ctGeofenceAPI)
        `when`(CTGeofenceAPI.getLogger()).thenReturn(logger)
        `when`(LocationServices.getGeofencingClient(application)).thenReturn(geofencingClient)
        `when`(ctGeofenceAPI.ctGeofenceAdapter).thenReturn(ctGeofenceAdapter)

    }

    @Test
    fun testAddAllGeofenceTC1() {

        // when fence list is null
        val geofenceAdapter = GoogleGeofenceAdapter(application)
        geofenceAdapter.addAllGeofence(null, onSuccessListener)

        verify(geofencingClient, never()).addGeofences(
            any(
                GeofencingRequest::class.java
            ), any(PendingIntent::class.java)
        )
    }

    @Test
    fun testAddAllGeofenceTC2() {
        // when fence list is empty
        val geofenceAdapter = GoogleGeofenceAdapter(application)
        geofenceAdapter.addAllGeofence(listOf(), onSuccessListener)
        verify(geofencingClient, never()).addGeofences(
            any(GeofencingRequest::class.java), any(PendingIntent::class.java)
        )
    }

    @Test
    fun testAddAllGeofenceTC3() {

        // when fence list is not empty

        val ctGeofences = CTGeofence.from(GeofenceJSON.geofence)
        val geofenceAdapter = GoogleGeofenceAdapter(application)
        `when`(
            geofencingClient.addGeofences(
                any(GeofencingRequest::class.java), any(PendingIntent::class.java)
            )
        ).thenReturn(task)
        geofenceAdapter.addAllGeofence(ctGeofences, onSuccessListener)

        tasksMockedStatic.verify { Tasks.await(task) }
        verify(onSuccessListener).onSuccess(null)
    }

    @Test
    fun testGetGeofencingRequest() {
        val geofenceAdapter = GoogleGeofenceAdapter(application)
        val ctGeofences = CTGeofence.from(GeofenceJSON.geofence)

        geofenceAdapter.addAllGeofence(ctGeofences, onSuccessListener)
        val geofencingRequestArgumentCaptor = ArgumentCaptor.forClass(
            GeofencingRequest::class.java
        )
        val pendingIntentArgumentCaptor = ArgumentCaptor.forClass(PendingIntent::class.java)

        verify(geofencingClient).addGeofences(
            geofencingRequestArgumentCaptor.capture(), pendingIntentArgumentCaptor.capture()
        )
        assertEquals(
            GeofencingRequest.INITIAL_TRIGGER_ENTER,
            geofencingRequestArgumentCaptor.getValue().initialTrigger
        )
    }

    @Test
    fun testRemoveAllGeofenceTC1() {
        // when fence list is null
        val geofenceAdapter = GoogleGeofenceAdapter(application)
        geofenceAdapter.removeAllGeofence(null, onSuccessListener)

        verify(geofencingClient, never()).removeGeofences(anyList())
    }

    //
    @Test
    fun testRemoveAllGeofenceTC2() {
        // when fence list is empty
        val geofenceAdapter = GoogleGeofenceAdapter(application)
        geofenceAdapter.removeAllGeofence(listOf(), onSuccessListener)

        verify(geofencingClient, never()).removeGeofences(anyList())
    }

    //
    @Test
    fun testRemoveAllGeofenceTC3() {
        // when fence list is not empty

        val ctGeofenceIds = listOf("111", "222")
        val geofenceAdapter = GoogleGeofenceAdapter(application)
        `when`(geofencingClient.removeGeofences(ctGeofenceIds)).thenReturn(task)
        geofenceAdapter.removeAllGeofence(ctGeofenceIds, onSuccessListener)

        tasksMockedStatic.verify { Tasks.await(task) }
        verify(onSuccessListener).onSuccess(null)
    }

    @Test
    fun testStopGeofenceMonitoringTC1() {
        // when pending intent is null

        val geofenceAdapter = GoogleGeofenceAdapter(application)
        geofenceAdapter.stopGeofenceMonitoring(null)

        verify(geofencingClient, never()).removeGeofences(any(PendingIntent::class.java))
    }

    @Test
    fun testStopGeofenceMonitoringTC2() {
        // when pending intent is not null

        val geofenceAdapter = GoogleGeofenceAdapter(application)

        `when`(geofencingClient.removeGeofences(pendingIntent)).thenReturn(task)

        geofenceAdapter.stopGeofenceMonitoring(pendingIntent)

        tasksMockedStatic.verify { Tasks.await(task) }
        verify(pendingIntent).cancel()
    }
}
