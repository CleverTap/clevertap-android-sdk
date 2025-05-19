package com.clevertap.android.geofence

import android.app.PendingIntent
import android.content.Context
import com.clevertap.android.geofence.interfaces.CTGeofenceTask
import com.clevertap.android.geofence.interfaces.CTLocationAdapter
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

class LocationUpdateTaskTest : BaseTestCase() {

    @Mock
    lateinit var ctGeofenceAPI: CTGeofenceAPI

    @Mock
    lateinit var ctLocationAdapter: CTLocationAdapter

    @Mock
    lateinit var onCompleteListener: CTGeofenceTask.OnCompleteListener

    @Mock
    lateinit var logger: Logger

    @Mock
    lateinit var pendingIntent: PendingIntent

    private lateinit var ctGeofenceAPIMockedStatic: MockedStatic<CTGeofenceAPI>
    private lateinit var fileUtilsMockedStatic: MockedStatic<FileUtils>
    private lateinit var utilsMockedStatic: MockedStatic<Utils>

    private lateinit var pendingIntentFactoryMockedStatic: MockedStatic<PendingIntentFactory>

    @Before
    override fun setUp() {

        MockitoAnnotations.openMocks(this)

        ctGeofenceAPIMockedStatic = mockStatic(CTGeofenceAPI::class.java)
        fileUtilsMockedStatic = mockStatic(FileUtils::class.java)
        utilsMockedStatic = mockStatic(Utils::class.java)
        pendingIntentFactoryMockedStatic = mockStatic(PendingIntentFactory::class.java)
        super.setUp()

        `when`(CTGeofenceAPI.getInstance(application)).thenReturn(ctGeofenceAPI)
        `when`(CTGeofenceAPI.getLogger()).thenReturn(logger)
        `when`(ctGeofenceAPI.ctLocationAdapter).thenReturn(ctLocationAdapter)
    }

    @After
    fun cleanup() {
        ctGeofenceAPIMockedStatic.close()
        fileUtilsMockedStatic.close()
        utilsMockedStatic.close()
        pendingIntentFactoryMockedStatic.close()
    }

    @Test
    fun testExecuteTC1() {
        // when pending intent is null and bgLocationUpdate is true

        val ctGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(true).build()
        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(ctGeofenceSettings)

        val task = LocationUpdateTask(application)
        task.setOnCompleteListener(onCompleteListener)
        task.execute()

        verify(ctLocationAdapter).requestLocationUpdates()

        utilsMockedStatic.verify {
            Utils.writeSettingsToFile(
                any(Context::class.java), any(CTGeofenceSettings::class.java)
            )
        }

        verify(onCompleteListener).onComplete()
    }

    @Test
    fun testExecuteTC2() {
        // when pending intent is null and bgLocationUpdate is false

        val ctGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(false).build()

        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(ctGeofenceSettings)

        val task = LocationUpdateTask(application)
        task.execute()

        verify(ctLocationAdapter, never()).requestLocationUpdates()
        verify(ctLocationAdapter, never()).removeLocationUpdates(
            any(PendingIntent::class.java)
        )

        utilsMockedStatic.verify {
            Utils.writeSettingsToFile(
                any(Context::class.java), any(CTGeofenceSettings::class.java)
            )
        }
    }

    @Test
    fun testExecuteTC3() {
        // when pending intent is not null and bgLocationUpdate is false

        val ctGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(false).build()

        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(ctGeofenceSettings)

        // make pending intent non-null
        `when`(
            PendingIntentFactory.getPendingIntent(any(Context::class.java), anyInt(), anyInt())
        ).thenReturn(pendingIntent)

        val task = LocationUpdateTask(application)
        task.execute()

        // verify that previous update is removed
        verify(ctLocationAdapter).removeLocationUpdates(any(PendingIntent::class.java))
        verify(ctLocationAdapter, never()).requestLocationUpdates()

        utilsMockedStatic.verify {
            Utils.writeSettingsToFile(
                any(Context::class.java), any(CTGeofenceSettings::class.java)
            )
        }

    }

    @Test
    fun testExecuteWhenLocationAdapterIsNull() {
        // when location adapter is null

        `when`(ctGeofenceAPI.ctLocationAdapter).thenReturn(null)
        val task = LocationUpdateTask(application)
        task.execute()

        utilsMockedStatic.verifyNoInteractions()
    }

    @Test
    fun testIsRequestLocationTC10() {
        // when currentBgLocationUpdate is true and there is no change in settings

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(true).build()

        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(currentGeofenceSettings)
        `when`(
            PendingIntentFactory.getPendingIntent(any(Context::class.java), anyInt(), anyInt())
        ).thenReturn(pendingIntent)

        val lastGeofenceSettings = CTGeofenceSettings.Builder().build()

        `when`(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings)

        val task = LocationUpdateTask(application)
        task.execute()

        verify(logger).verbose(
            CTGeofenceAPI.GEOFENCE_LOG_TAG, "Dropping duplicate location update request"
        )
    }

    @Test
    fun testIsRequestLocationTC2() {
        // when currentBgLocationUpdate is true and pendingIntent is null

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(true).build()

        `when`(
            PendingIntentFactory.getPendingIntent(
                any(Context::class.java), anyInt(), anyInt()
            )
        ).thenReturn(null)
        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(currentGeofenceSettings)

        val lastGeofenceSettings = CTGeofenceSettings.Builder().build()

        `when`(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings)

        val task = LocationUpdateTask(application)
        task.execute()

        verify(ctLocationAdapter).requestLocationUpdates()
    }

    @Test
    fun testIsRequestLocationTC3() {
        // when fetch mode is current and change in accuracy

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .setLocationAccuracy(CTGeofenceSettings.ACCURACY_HIGH).build()

        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(currentGeofenceSettings)
        `when`(
            PendingIntentFactory.getPendingIntent(
                any(Context::class.java), anyInt(), anyInt()
            )
        ).thenReturn(pendingIntent)

        val lastGeofenceSettings =
            CTGeofenceSettings.Builder().setLocationAccuracy(CTGeofenceSettings.ACCURACY_LOW)
                .build()

        `when`(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings)

        val task = LocationUpdateTask(application)
        task.execute()

        verify(ctLocationAdapter).requestLocationUpdates()
    }

    @Test
    fun testIsRequestLocationTC4() {
        // when fetch mode is current and change in interval

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .setInterval(2000000).build()

        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(currentGeofenceSettings)
        `when`(
            PendingIntentFactory.getPendingIntent(
                any(Context::class.java), anyInt(), anyInt()
            )
        ).thenReturn(pendingIntent)


        val lastGeofenceSettings = CTGeofenceSettings.Builder().setInterval(5000000).build()

        `when`(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings)

        val task = LocationUpdateTask(application)
        task.execute()

        verify(ctLocationAdapter).requestLocationUpdates()
    }

    @Test
    fun testIsRequestLocationTC5() {
        // when fetch mode is current and change in fastest interval

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .setFastestInterval(2000000).build()

        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(currentGeofenceSettings)
        `when`(
            PendingIntentFactory.getPendingIntent(
                any(Context::class.java), anyInt(), anyInt()
            )
        ).thenReturn(pendingIntent)

        val lastGeofenceSettings = CTGeofenceSettings.Builder().setFastestInterval(5000000).build()

        `when`(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings)

        val task = LocationUpdateTask(application)
        task.execute()

        verify(ctLocationAdapter).requestLocationUpdates()
    }

    @Test
    fun testIsRequestLocationTC6() {
        // when fetch mode is current and change in displacement

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC)
                .setSmallestDisplacement(600f).build()

        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(currentGeofenceSettings)
        `when`(
            PendingIntentFactory.getPendingIntent(
                any(Context::class.java), anyInt(), anyInt()
            )
        ).thenReturn(pendingIntent)

        val lastGeofenceSettings =
            CTGeofenceSettings.Builder().setSmallestDisplacement(700f).build()

        `when`(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings)

        val task = LocationUpdateTask(application)
        task.execute()

        verify(ctLocationAdapter).requestLocationUpdates()
    }

    @Test
    fun testIsRequestLocationTC7() {
        // when fetch mode is last location and change in interval

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC)
                .setInterval(2000000).build()

        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(currentGeofenceSettings)

        val lastGeofenceSettings = CTGeofenceSettings.Builder().setInterval(5000000).build()

        `when`(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings)
        `when`(
            PendingIntentFactory.getPendingIntent(
                any(Context::class.java), anyInt(), anyInt()
            )
        ).thenReturn(pendingIntent)

        val task = LocationUpdateTask(application)
        task.execute()

        verify(ctLocationAdapter).requestLocationUpdates()
    }

    @Test
    fun testIsRequestLocationTC8() {
        // when fetch mode is current location and change in fetch mode

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC).build()

        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(currentGeofenceSettings)

        val lastGeofenceSettings = CTGeofenceSettings.Builder()
            .setLocationFetchMode(CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC).build()

        `when`(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings)
        `when`(
            PendingIntentFactory.getPendingIntent(
                any(Context::class.java), anyInt(), anyInt()
            )
        ).thenReturn(pendingIntent)

        val task = LocationUpdateTask(application)
        task.execute()

        verify(ctLocationAdapter).requestLocationUpdates()
    }

    @Test
    fun testIsRequestLocationTC9() {
        // when fetch mode is last location and change in fetch mode

        val currentGeofenceSettings =
            CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(true)
                .setLocationFetchMode(CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC).build()

        `when`(ctGeofenceAPI.geofenceSettings).thenReturn(currentGeofenceSettings)

        val lastGeofenceSettings = CTGeofenceSettings.Builder()
            .setLocationFetchMode(CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC).build()

        `when`(Utils.readSettingsFromFile(application)).thenReturn(lastGeofenceSettings)
        `when`(
            PendingIntentFactory.getPendingIntent(
                any(Context::class.java), anyInt(), anyInt()
            )
        ).thenReturn(pendingIntent)

        val task = LocationUpdateTask(application)
        task.execute()

        verify(ctLocationAdapter).requestLocationUpdates()
    }
}
