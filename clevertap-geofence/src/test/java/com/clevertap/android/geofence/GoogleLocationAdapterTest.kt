package com.clevertap.android.geofence

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.clevertap.android.geofence.CTGeofenceConstants.TAG_WORK_LOCATION_UPDATES
import com.clevertap.android.geofence.fakes.GeofenceEventFake
import com.clevertap.android.geofence.interfaces.CTLocationAdapter
import com.clevertap.android.sdk.CleverTapAPI
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.util.concurrent.TimeUnit


class GoogleLocationAdapterTest : BaseTestCase() {

    @Mock
    lateinit var cleverTapAPI: CleverTapAPI

    @Mock
    lateinit var ctGeofenceAPI: CTGeofenceAPI

    @Mock
    lateinit var ctLocationAdapter: CTLocationAdapter

    @Mock
    lateinit var providerClient: FusedLocationProviderClient

    @Mock
    lateinit var logger: Logger

    private lateinit var tasksMockedStatic: MockedStatic<Tasks>

    private lateinit var utilsMockedStatic: MockedStatic<Utils>

    private lateinit var locationServicesMockedStatic: MockedStatic<LocationServices>

    private lateinit var ctGeofenceAPIMockedStatic: MockedStatic<CTGeofenceAPI>

    @Before
    override fun setUp() {

        MockitoAnnotations.openMocks(this)

        ctGeofenceAPIMockedStatic = mockStatic(
            CTGeofenceAPI::class.java
        )
        locationServicesMockedStatic = mockStatic(LocationServices::class.java)
        utilsMockedStatic = mockStatic(Utils::class.java)
        tasksMockedStatic = mockStatic(Tasks::class.java)

        `when`(CTGeofenceAPI.getInstance(any(Context::class.java))).thenReturn(ctGeofenceAPI)
        `when`(CTGeofenceAPI.getLogger()).thenReturn(logger)
        `when`(ctGeofenceAPI.ctLocationAdapter).thenReturn(ctLocationAdapter)
        `when`(ctGeofenceAPI.cleverTapApi).thenReturn(cleverTapAPI)

        super.setUp()

        val config = Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor()).build()

        // Initialize WorkManager for unit test.
        WorkManagerTestInitHelper.initializeTestWorkManager(
            application, config
        )
    }

    @After
    fun cleanup() {
        ctGeofenceAPIMockedStatic.close()
        locationServicesMockedStatic.close()
        utilsMockedStatic.close()
        tasksMockedStatic.close()
    }

    @Test
    fun testApplySettings() {
        val ctGeofenceSettings = CTGeofenceSettings.Builder()
            .setLocationFetchMode(CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC)
            .enableBackgroundLocationUpdates(true).setInterval(2000000).setFastestInterval(2000000)
            .setLocationAccuracy(CTGeofenceSettings.ACCURACY_LOW).setSmallestDisplacement(900f)
            .build()

        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(ctGeofenceSettings)

        val locationAdapter = GoogleLocationAdapter(application)

        locationAdapter.requestLocationUpdates()
        val actualLocationRequest =
            LocationRequest.Builder(Priority.PRIORITY_LOW_POWER, 2000000)
                .setMinUpdateIntervalMillis(2000000)
                .setMinUpdateDistanceMeters(900f)
                .build()

        assertEquals(
            ctGeofenceSettings.interval, actualLocationRequest.intervalMillis
        )
        assertEquals(
            ctGeofenceSettings.fastestInterval,
            actualLocationRequest.minUpdateIntervalMillis
        )
        assertEquals(
            ctGeofenceSettings.smallestDisplacement,
            actualLocationRequest.minUpdateDistanceMeters,
            0f
        )
        assertEquals(Priority.PRIORITY_LOW_POWER, actualLocationRequest.priority)
    }

    @Test
    fun testGetLastLocation() {
        val expectedLocation = GeofenceEventFake.getTriggeredLocation()
        `when`(LocationServices.getFusedLocationProviderClient(application)).thenReturn(
            providerClient
        )
        val locationTask = mock(Task::class.java) as Task<Location>
        `when`(Tasks.await(locationTask)).thenReturn(expectedLocation)

        `when`(providerClient.lastLocation).thenReturn(locationTask)
        val locationAdapter = GoogleLocationAdapter(application)

        locationAdapter.getLastLocation { location ->
            Assert.assertSame(
                expectedLocation,
                location
            )
        }
    }

    @Test
    fun testRequestLocationUpdatesTC1() {
        // when backgroundLocationUpdates not enabled

        `when`(LocationServices.getFusedLocationProviderClient(application)).thenReturn(
            providerClient
        )

        val locationAdapter = GoogleLocationAdapter(application)

        val ctGeofenceSettings = CTGeofenceSettings.Builder()
            .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
            .enableBackgroundLocationUpdates(false)
            .build()

        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(ctGeofenceSettings)

        locationAdapter.requestLocationUpdates()
        verify(cleverTapAPI).pushGeoFenceError(anyInt(), anyString())
        verify(providerClient, never()).requestLocationUpdates(
            any(LocationRequest::class.java), any(PendingIntent::class.java)
        )
    }

    @Test
    fun testRequestLocationUpdatesTC2() {
        // when backgroundLocationUpdates is enabled and fetch mode is current location

        `when`(LocationServices.getFusedLocationProviderClient(application)).thenReturn(
            providerClient
        )
        `when`(Utils.isConcurrentFuturesDependencyAvailable()).thenReturn(true)

        val locationAdapter = GoogleLocationAdapter(application)

        val ctGeofenceSettings = CTGeofenceSettings.Builder()
            .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
            .enableBackgroundLocationUpdates(true).build()

        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(ctGeofenceSettings)

        // enqueue work to verify later that work is cancelled by method under test
        val workManager = WorkManager.getInstance(application)

        val locationRequest = PeriodicWorkRequest.Builder(
            BackgroundLocationWork::class.java,
            1800000, TimeUnit.MILLISECONDS,
            600000, TimeUnit.MILLISECONDS
        ).build()

        workManager.enqueueUniquePeriodicWork(
            TAG_WORK_LOCATION_UPDATES, ExistingPeriodicWorkPolicy.KEEP, locationRequest
        ).result.get()

        // call method under test
        locationAdapter.requestLocationUpdates()

        // Get WorkInfo and outputData
        val workInfo = workManager.getWorkInfoById(locationRequest.id).get()
        // Assert work is cancelled by locationAdapter.requestLocationUpdates()
        assertEquals(WorkInfo.State.CANCELLED, workInfo.state)

        verify(providerClient).requestLocationUpdates(
            any(
                LocationRequest::class.java
            ),
            any(PendingIntent::class.java)
        )
        tasksMockedStatic.verify { Tasks.await(any()) }

    }

    @Test
    fun testRequestLocationUpdatesTC3() {

        // when backgroundLocationUpdates is enabled and fetch mode is last location

        `when`(LocationServices.getFusedLocationProviderClient(application)).thenReturn(
            providerClient
        )
        `when`(Utils.isConcurrentFuturesDependencyAvailable()).thenReturn(true)

        val locationAdapter = GoogleLocationAdapter(application)

        val ctGeofenceSettings = CTGeofenceSettings.Builder()
            .setLocationFetchMode(CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC)
            .enableBackgroundLocationUpdates(true).build()

        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(ctGeofenceSettings)

        // simulate current location request exists in OS
        PendingIntentFactory.getPendingIntent(
            application, PendingIntentFactory.PENDING_INTENT_LOCATION, FLAG_UPDATE_CURRENT
        )

        // call method under test
        locationAdapter.requestLocationUpdates()

        val workManager = WorkManager.getInstance(application)

        // Get WorkInfo and outputData
        val workInfos = workManager.getWorkInfosForUniqueWork(TAG_WORK_LOCATION_UPDATES).get()
        // Assert work is enqueued by locationAdapter.requestLocationUpdates()
        assertTrue(
            setOf(WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING).contains(workInfos[0].state)
        )

        verify(providerClient).removeLocationUpdates(any(PendingIntent::class.java))

        tasksMockedStatic.verify { Tasks.await(any()) }
    }
}
