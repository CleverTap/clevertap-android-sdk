package com.clevertap.android.geofence

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class GoogleLocationAdapterTest : BaseTestCase() {

    private lateinit var cleverTapAPI: CleverTapAPI
    private lateinit var ctGeofenceAPI: CTGeofenceAPI
    private lateinit var ctLocationAdapter: CTLocationAdapter
    private lateinit var providerClient: FusedLocationProviderClient
    private lateinit var logger: Logger

    @Before
    override fun setUp() {
        super.setUp()

        cleverTapAPI = mockk(relaxed = true)
        ctGeofenceAPI = mockk(relaxed = true)
        ctLocationAdapter = mockk(relaxed = true)
        providerClient = mockk(relaxed = true)
        logger = mockk(relaxed = true)

        mockkStatic(CTGeofenceAPI::class)
        mockkStatic(LocationServices::class)
        mockkStatic(Utils::class)
        mockkStatic(Tasks::class)

        every { CTGeofenceAPI.getInstance(any()) } returns ctGeofenceAPI
        every { CTGeofenceAPI.getLogger() } returns logger
        every { ctGeofenceAPI.ctLocationAdapter } returns ctLocationAdapter
        every { ctGeofenceAPI.cleverTapApi } returns cleverTapAPI

        val config = Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor()).build()

        // Initialize WorkManager for unit test.
        WorkManagerTestInitHelper.initializeTestWorkManager(
            application, config
        )
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
    fun testApplySettings() {
        val ctGeofenceSettings = CTGeofenceSettings.Builder()
            .setLocationFetchMode(CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC)
            .enableBackgroundLocationUpdates(true).setInterval(2000000).setFastestInterval(2000000)
            .setLocationAccuracy(CTGeofenceSettings.ACCURACY_LOW).setSmallestDisplacement(900f)
            .build()

        every { ctGeofenceAPI.geofenceSettings } returns ctGeofenceSettings

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
        every { LocationServices.getFusedLocationProviderClient(application) } returns providerClient

        val locationTask = mockk<Task<Location>>(relaxed = true)
        every { Tasks.await(locationTask) } returns expectedLocation
        every { providerClient.lastLocation } returns locationTask

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

        every { LocationServices.getFusedLocationProviderClient(application) } returns providerClient

        val locationAdapter = GoogleLocationAdapter(application)

        val ctGeofenceSettings = CTGeofenceSettings.Builder()
            .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
            .enableBackgroundLocationUpdates(false)
            .build()

        every { ctGeofenceAPI.geofenceSettings } returns ctGeofenceSettings

        locationAdapter.requestLocationUpdates()

        verify { cleverTapAPI.pushGeoFenceError(any(), any()) }
        verify(exactly = 0) { providerClient.requestLocationUpdates(any(), any()) }
    }

    @Test
    fun testRequestLocationUpdatesTC2() {
        // when backgroundLocationUpdates is enabled and fetch mode is current location

        every { LocationServices.getFusedLocationProviderClient(application) } returns providerClient
        every { Utils.isConcurrentFuturesDependencyAvailable() } returns true

        val locationAdapter = GoogleLocationAdapter(application)

        val ctGeofenceSettings = CTGeofenceSettings.Builder()
            .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
            .enableBackgroundLocationUpdates(true).build()

        every { ctGeofenceAPI.geofenceSettings } returns ctGeofenceSettings

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
        assertEquals(WorkInfo.State.CANCELLED, workInfo?.state)

        verify { providerClient.requestLocationUpdates(any(), any()) }
        verify { Tasks.await(any()) }
    }

    @Test
    fun testRequestLocationUpdatesTC3() {
        // when backgroundLocationUpdates is enabled and fetch mode is last location

        every { LocationServices.getFusedLocationProviderClient(application) } returns providerClient
        every { Utils.isConcurrentFuturesDependencyAvailable() } returns true

        val locationAdapter = GoogleLocationAdapter(application)

        val ctGeofenceSettings = CTGeofenceSettings.Builder()
            .setLocationFetchMode(CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC)
            .enableBackgroundLocationUpdates(true).build()

        every { ctGeofenceAPI.geofenceSettings } returns ctGeofenceSettings

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

        verify { providerClient.removeLocationUpdates(any<PendingIntent>()) }
        verify { Tasks.await(any()) }
    }
}
