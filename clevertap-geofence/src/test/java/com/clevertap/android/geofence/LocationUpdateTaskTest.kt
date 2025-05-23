package com.clevertap.android.geofence

import android.app.PendingIntent
import com.clevertap.android.geofence.interfaces.CTGeofenceTask
import com.clevertap.android.geofence.interfaces.CTLocationAdapter
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class LocationUpdateTaskTest : BaseTestCase() {

    private lateinit var ctGeofenceAPI: CTGeofenceAPI
    private lateinit var ctLocationAdapter: CTLocationAdapter
    private lateinit var onCompleteListener: CTGeofenceTask.OnCompleteListener
    private lateinit var logger: Logger
    private lateinit var pendingIntent: PendingIntent

    @Before
    override fun setUp() {
        super.setUp()

        ctGeofenceAPI = mockk(relaxed = true)
        ctLocationAdapter = mockk(relaxed = true)
        onCompleteListener = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        pendingIntent = mockk(relaxed = true)

        mockkStatic(CTGeofenceAPI::class)
        mockkStatic(FileUtils::class)
        mockkStatic(Utils::class)
        mockkStatic(PendingIntentFactory::class)

        every { CTGeofenceAPI.getInstance(application) } returns ctGeofenceAPI
        every { CTGeofenceAPI.getLogger() } returns logger
        every { ctGeofenceAPI.ctLocationAdapter } returns ctLocationAdapter
    }

    @After
    override fun cleanUp() {
        super.cleanUp()
        unmockkStatic(CTGeofenceAPI::class)
        unmockkStatic(FileUtils::class)
        unmockkStatic(Utils::class)
        unmockkStatic(PendingIntentFactory::class)
    }

    @Test
    fun testExecuteTC1() {
        // when pending intent is null and bgLocationUpdate is true

        val ctGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(true).build()
        every { ctGeofenceAPI.geofenceSettings } returns ctGeofenceSettings

        val task = LocationUpdateTask(application)
        task.setOnCompleteListener(onCompleteListener)
        task.execute()

        verify { ctLocationAdapter.requestLocationUpdates() }

        verify { Utils.writeSettingsToFile(any(), any()) }

        verify { onCompleteListener.onComplete() }
    }

    @Test
    fun testExecuteTC2() {
        // when pending intent is null and bgLocationUpdate is false

        val ctGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(false).build()

        every { ctGeofenceAPI.geofenceSettings } returns ctGeofenceSettings

        val task = LocationUpdateTask(application)
        task.execute()

        verify(exactly = 0) { ctLocationAdapter.requestLocationUpdates() }
        verify(exactly = 0) { ctLocationAdapter.removeLocationUpdates(any()) }
        verify { Utils.writeSettingsToFile(any(), any()) }
    }

    @Test
    fun testExecuteTC3() {
        // when pending intent is not null and bgLocationUpdate is false

        val ctGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(false).build()

        every { ctGeofenceAPI.geofenceSettings } returns ctGeofenceSettings

        // make pending intent non-null
        every { PendingIntentFactory.getPendingIntent(any(), any(), any()) } returns pendingIntent

        val task = LocationUpdateTask(application)
        task.execute()

        // verify that previous update is removed
        verify { ctLocationAdapter.removeLocationUpdates(any()) }
        verify(exactly = 0) { ctLocationAdapter.requestLocationUpdates() }
        verify { Utils.writeSettingsToFile(any(), any()) }
    }

    @Test
    fun testExecuteWhenLocationAdapterIsNull() {
        // when location adapter is null

        every { ctGeofenceAPI.ctLocationAdapter } returns null
        val task = LocationUpdateTask(application)
        task.execute()

        verify(exactly = 0) { Utils.writeSettingsToFile(any(), any()) }
    }

    @Test
    fun testIsRequestLocationTC10() {
        // when currentBgLocationUpdate is true and there is no change in settings

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(true).build()

        every { ctGeofenceAPI.geofenceSettings } returns currentGeofenceSettings
        every { PendingIntentFactory.getPendingIntent(any(), any(), any()) } returns pendingIntent

        val lastGeofenceSettings = CTGeofenceSettings.Builder().build()

        every { Utils.readSettingsFromFile(application) } returns lastGeofenceSettings

        val task = LocationUpdateTask(application)
        task.execute()

        verify {
            logger.verbose(
                CTGeofenceAPI.GEOFENCE_LOG_TAG, "Dropping duplicate location update request"
            )
        }
    }

    @Test
    fun testIsRequestLocationTC2() {
        // when currentBgLocationUpdate is true and pendingIntent is null

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(true).build()

        every { PendingIntentFactory.getPendingIntent(any(), any(), any()) } returns null
        every { ctGeofenceAPI.geofenceSettings } returns currentGeofenceSettings

        val lastGeofenceSettings = CTGeofenceSettings.Builder().build()

        every { Utils.readSettingsFromFile(application) } returns lastGeofenceSettings

        val task = LocationUpdateTask(application)
        task.execute()

        verify { ctLocationAdapter.requestLocationUpdates() }
    }

    @Test
    fun testIsRequestLocationTC3() {
        // when fetch mode is current and change in accuracy

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .setLocationAccuracy(CTGeofenceSettings.ACCURACY_HIGH).build()

        every { ctGeofenceAPI.geofenceSettings } returns currentGeofenceSettings
        every { PendingIntentFactory.getPendingIntent(any(), any(), any()) } returns pendingIntent

        val lastGeofenceSettings =
            CTGeofenceSettings.Builder().setLocationAccuracy(CTGeofenceSettings.ACCURACY_LOW)
                .build()

        every { Utils.readSettingsFromFile(application) } returns lastGeofenceSettings

        val task = LocationUpdateTask(application)
        task.execute()

        verify { ctLocationAdapter.requestLocationUpdates() }
    }

    @Test
    fun testIsRequestLocationTC4() {
        // when fetch mode is current and change in interval

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .setInterval(2000000).build()

        every { ctGeofenceAPI.geofenceSettings } returns currentGeofenceSettings
        every { PendingIntentFactory.getPendingIntent(any(), any(), any()) } returns pendingIntent


        val lastGeofenceSettings = CTGeofenceSettings.Builder().setInterval(5000000).build()
        every { Utils.readSettingsFromFile(application) } returns lastGeofenceSettings

        val task = LocationUpdateTask(application)
        task.execute()

        verify { ctLocationAdapter.requestLocationUpdates() }
    }

    @Test
    fun testIsRequestLocationTC5() {
        // when fetch mode is current and change in fastest interval

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .setFastestInterval(2000000).build()

        every { ctGeofenceAPI.geofenceSettings } returns currentGeofenceSettings
        every { PendingIntentFactory.getPendingIntent(any(), any(), any()) } returns pendingIntent

        val lastGeofenceSettings = CTGeofenceSettings.Builder().setFastestInterval(5000000).build()

        every { Utils.readSettingsFromFile(application) } returns lastGeofenceSettings

        val task = LocationUpdateTask(application)
        task.execute()

        verify { ctLocationAdapter.requestLocationUpdates() }
    }

    @Test
    fun testIsRequestLocationTC6() {
        // when fetch mode is current and change in displacement

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .setSmallestDisplacement(600f).build()

        every { ctGeofenceAPI.geofenceSettings } returns currentGeofenceSettings
        every { PendingIntentFactory.getPendingIntent(any(), any(), any()) } returns pendingIntent

        val lastGeofenceSettings =
            CTGeofenceSettings.Builder().setSmallestDisplacement(700f).build()

        every { Utils.readSettingsFromFile(application) } returns lastGeofenceSettings

        val task = LocationUpdateTask(application)
        task.execute()

        verify { ctLocationAdapter.requestLocationUpdates() }
    }

    @Test
    fun testIsRequestLocationTC7() {
        // when fetch mode is last location and change in interval

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC)
                .setInterval(2000000).build()

        every { ctGeofenceAPI.geofenceSettings } returns currentGeofenceSettings

        val lastGeofenceSettings = CTGeofenceSettings.Builder().setInterval(5000000).build()

        every { Utils.readSettingsFromFile(application) } returns lastGeofenceSettings
        every { PendingIntentFactory.getPendingIntent(any(), any(), any()) } returns pendingIntent

        val task = LocationUpdateTask(application)
        task.execute()

        verify { ctLocationAdapter.requestLocationUpdates() }
    }

    @Test
    fun testIsRequestLocationTC8() {
        // when fetch mode is current location and change in fetch mode

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC).build()

        every { ctGeofenceAPI.geofenceSettings } returns currentGeofenceSettings

        val lastGeofenceSettings = CTGeofenceSettings.Builder()
            .setLocationFetchMode(CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC).build()

        every { Utils.readSettingsFromFile(application) } returns lastGeofenceSettings
        every { PendingIntentFactory.getPendingIntent(any(), any(), any()) } returns pendingIntent

        val task = LocationUpdateTask(application)
        task.execute()

        verify { ctLocationAdapter.requestLocationUpdates() }
    }

    @Test
    fun testIsRequestLocationTC9() {
        // when fetch mode is last location and change in fetch mode

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC).build()

        every { ctGeofenceAPI.geofenceSettings } returns currentGeofenceSettings

        val lastGeofenceSettings = CTGeofenceSettings.Builder()
            .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC).build()

        every { Utils.readSettingsFromFile(application) } returns lastGeofenceSettings
        every { PendingIntentFactory.getPendingIntent(any(), any(), any()) } returns pendingIntent

        val task = LocationUpdateTask(application)
        task.execute()

        verify { ctLocationAdapter.requestLocationUpdates() }
    }
}
