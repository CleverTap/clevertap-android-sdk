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
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GoogleGeofenceAdapterTest : BaseTestCase() {

    private lateinit var ctGeofenceAPI: CTGeofenceAPI
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var onSuccessListener: OnSuccessListener<Void>
    private lateinit var pendingIntent: PendingIntent
    private lateinit var task: Task<Void>
    private lateinit var ctGeofenceAdapter: CTGeofenceAdapter
    private lateinit var logger: Logger

    @Before
    override fun setUp() {
        super.setUp()

        ctGeofenceAPI = mockk(relaxed = true)
        geofencingClient = mockk(relaxed = true)
        onSuccessListener = mockk(relaxed = true)
        pendingIntent = mockk(relaxed = true)
        task = mockk(relaxed = true)
        ctGeofenceAdapter = mockk(relaxed = true)
        logger = mockk(relaxed = true)

        mockkStatic(CTGeofenceAPI::class)
        mockkStatic(LocationServices::class)
        mockkStatic(Utils::class)
        mockkStatic(Tasks::class)

        every { CTGeofenceAPI.getInstance(application) } returns ctGeofenceAPI
        every { CTGeofenceAPI.getLogger() } returns logger
        every { LocationServices.getGeofencingClient(application) } returns geofencingClient
        every { ctGeofenceAPI.ctGeofenceAdapter } returns ctGeofenceAdapter
    }

    @After
    override fun cleanUp() {
        super.cleanUp()
        unmockkStatic(CTGeofenceAPI::class)
        unmockkStatic(LocationServices::class)
        unmockkStatic(Utils::class)
        unmockkStatic(Tasks::class)
    }

    @Test
    fun testAddAllGeofenceTC1() {
        // when fence list is null
        val geofenceAdapter = GoogleGeofenceAdapter(application)
        geofenceAdapter.addAllGeofence(null, onSuccessListener)

        verify(exactly = 0) { geofencingClient.addGeofences(any(), any()) }
    }

    @Test
    fun testAddAllGeofenceTC2() {
        // when fence list is empty
        val geofenceAdapter = GoogleGeofenceAdapter(application)
        geofenceAdapter.addAllGeofence(listOf(), onSuccessListener)

        verify(exactly = 0) { geofencingClient.addGeofences(any(), any()) }
    }

    @Test
    fun testAddAllGeofenceTC3() {
        // when fence list is not empty

        val ctGeofences = CTGeofence.from(GeofenceJSON.geofence)
        val geofenceAdapter = GoogleGeofenceAdapter(application)

        every { geofencingClient.addGeofences(any(), any()) } returns task

        geofenceAdapter.addAllGeofence(ctGeofences, onSuccessListener)

        verify { Tasks.await(task) }
        verify { onSuccessListener.onSuccess(null) }
    }

    @Test
    fun testGetGeofencingRequest() {
        val geofenceAdapter = GoogleGeofenceAdapter(application)
        val ctGeofences = CTGeofence.from(GeofenceJSON.geofence)

        every { geofencingClient.addGeofences(any(), any()) } returns task

        geofenceAdapter.addAllGeofence(ctGeofences, onSuccessListener)

        val geofencingRequestSlot = slot<GeofencingRequest>()

        verify {
            geofencingClient.addGeofences(
                capture(geofencingRequestSlot),
                any()
            )
        }

        assertEquals(
            GeofencingRequest.INITIAL_TRIGGER_ENTER,
            geofencingRequestSlot.captured.initialTrigger
        )
    }

    @Test
    fun testRemoveAllGeofenceTC1() {
        // when fence list is null
        val geofenceAdapter = GoogleGeofenceAdapter(application)
        geofenceAdapter.removeAllGeofence(null, onSuccessListener)

        verify(exactly = 0) { geofencingClient.removeGeofences(any<List<String>>()) }
    }

    @Test
    fun testRemoveAllGeofenceTC2() {
        // when fence list is empty
        val geofenceAdapter = GoogleGeofenceAdapter(application)
        geofenceAdapter.removeAllGeofence(listOf(), onSuccessListener)

        verify(exactly = 0) { geofencingClient.removeGeofences(any<List<String>>()) }
    }

    @Test
    fun testRemoveAllGeofenceTC3() {
        // when fence list is not empty

        val ctGeofenceIds = listOf("111", "222")
        val geofenceAdapter = GoogleGeofenceAdapter(application)

        every { geofencingClient.removeGeofences(ctGeofenceIds) } returns task

        geofenceAdapter.removeAllGeofence(ctGeofenceIds, onSuccessListener)

        verify { Tasks.await(task) }
        verify { onSuccessListener.onSuccess(null) }
    }

    @Test
    fun testStopGeofenceMonitoringTC1() {
        // when pending intent is null

        val geofenceAdapter = GoogleGeofenceAdapter(application)
        geofenceAdapter.stopGeofenceMonitoring(null)

        verify(exactly = 0) { geofencingClient.removeGeofences(any<PendingIntent>()) }
    }

    @Test
    fun testStopGeofenceMonitoringTC2() {
        // when pending intent is not null
        justRun { Tasks.await<Any>(any()) }
        val geofenceAdapter = GoogleGeofenceAdapter(application)
        every { geofencingClient.removeGeofences(pendingIntent) } returns task

        geofenceAdapter.stopGeofenceMonitoring(pendingIntent)

        verify { Tasks.await(task) }
        verify { pendingIntent.cancel() }
    }
}
