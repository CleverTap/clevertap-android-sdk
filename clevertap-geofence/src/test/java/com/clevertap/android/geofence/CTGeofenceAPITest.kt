package com.clevertap.android.geofence

import android.Manifest
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.location.Location
import com.clevertap.android.geofence.CTGeofenceConstants.DEFAULT_LATITUDE
import com.clevertap.android.geofence.CTGeofenceConstants.DEFAULT_LONGITUDE
import com.clevertap.android.geofence.fakes.GeofenceJSON
import com.clevertap.android.geofence.interfaces.CTGeofenceAdapter
import com.clevertap.android.geofence.interfaces.CTGeofenceEventsListener
import com.clevertap.android.geofence.interfaces.CTLocationAdapter
import com.clevertap.android.geofence.interfaces.CTLocationUpdatesListener
import com.clevertap.android.sdk.CleverTapAPI
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.util.concurrent.ExecutorService

class CTGeofenceAPITest : BaseTestCase() {

    @Mock
    lateinit var cleverTapAPI: CleverTapAPI

    @Mock
    lateinit var executorService: ExecutorService

    @Mock
    lateinit var geofenceAdapter: CTGeofenceAdapter

    @Mock
    lateinit var locationAdapter: CTLocationAdapter

    private lateinit var ctGeofenceFactoryMockedStatic: MockedStatic<CTGeofenceFactory>
    private lateinit var ctLocationFactoryMockedStatic: MockedStatic<CTLocationFactory>
    private lateinit var utilsMockedStatic: MockedStatic<Utils>

    @After
    fun cleanup() {
        val instance = CTGeofenceAPI.getInstance(application)
        val field = CTGeofenceAPI::class.java.getDeclaredField("ctGeofenceAPI")
        field.isAccessible = true
        field.set(instance, null)

        val taskManagerInstance = CTGeofenceTaskManager.getInstance()
        val fieldTaskManager = CTGeofenceTaskManager::class.java.getDeclaredField("taskManager")
        fieldTaskManager.isAccessible = true
        fieldTaskManager.set(taskManagerInstance, null)

        ctLocationFactoryMockedStatic.close()
        ctGeofenceFactoryMockedStatic.close()
        utilsMockedStatic.close()
    }

    @Before
    override fun setUp() {
        MockitoAnnotations.openMocks(this)
        super.setUp()

        ctLocationFactoryMockedStatic = mockStatic(CTLocationFactory::class.java)
        ctLocationFactoryMockedStatic.`when`<CTLocationAdapter> {
            CTLocationFactory.createLocationAdapter(
                application
            )
        }.thenReturn(locationAdapter)

        ctGeofenceFactoryMockedStatic = mockStatic(CTGeofenceFactory::class.java)
        ctGeofenceFactoryMockedStatic.`when`<CTGeofenceAdapter> {
            CTGeofenceFactory.createGeofenceAdapter(
                application
            )
        }.thenReturn(geofenceAdapter)

        utilsMockedStatic = mockStatic(Utils::class.java)
    }

    @Test
    fun testDeactivate() {
        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        mockStatic(FileUtils::class.java).use { fileUtilsMockedStatic ->
            ctGeofenceAPI.deactivate()
            val argumentCaptor = ArgumentCaptor.forClass(Runnable::class.java)
            verify(executorService).submit(argumentCaptor.capture())

            val geofenceMonitoring = PendingIntentFactory.getPendingIntent(
                application, PendingIntentFactory.PENDING_INTENT_GEOFENCE, FLAG_UPDATE_CURRENT
            )
            val locationUpdates = PendingIntentFactory.getPendingIntent(
                application, PendingIntentFactory.PENDING_INTENT_LOCATION, FLAG_UPDATE_CURRENT
            )

            argumentCaptor.getValue().run()

            verify(geofenceAdapter).stopGeofenceMonitoring(geofenceMonitoring)
            verify(locationAdapter).removeLocationUpdates(locationUpdates)

            fileUtilsMockedStatic.verify {
                FileUtils.deleteDirectory(
                    any(), FileUtils.getCachedDirName(application)
                )
            }

            assertFalse(ctGeofenceAPI.isActivated)
        }
    }

    @Test
    fun testGetInstance() {
        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        assertNotNull(ctGeofenceAPI)

        val ctGeofenceAPI1 = CTGeofenceAPI.getInstance(application)
        assertSame(ctGeofenceAPI, ctGeofenceAPI1)
    }

    @Test
    fun testGetLogger() {
        assertNotNull(CTGeofenceAPI.getLogger())
    }

    @Test
    fun testHandleGeoFencesTC1() {
        // when location access permission is not granted
        `when`(
            Utils.hasPermission(
                application, Manifest.permission.ACCESS_FINE_LOCATION
            )
        ).thenReturn(false)

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        ctGeofenceAPI.handleGeoFences(GeofenceJSON.geofence)

        verify(executorService, never()).submit(any(Runnable::class.java))
    }

    @Test
    fun testHandleGeoFencesTC2() {
        // when background location permission is not granted
        `when`(
            Utils.hasPermission(
                application, Manifest.permission.ACCESS_FINE_LOCATION
            )
        ).thenReturn(true)
        `when`(Utils.hasBackgroundLocationPermission(application)).thenReturn(false)

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        ctGeofenceAPI.handleGeoFences(GeofenceJSON.geofence)

        verify(executorService, never()).submit(any(Runnable::class.java))
    }

    @Test
    fun testHandleGeoFencesTC3() {
        // when geofence list is null
        `when`(
            Utils.hasPermission(
                application, Manifest.permission.ACCESS_FINE_LOCATION
            )
        ).thenReturn(true)
        `when`(Utils.hasBackgroundLocationPermission(application)).thenReturn(true)

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        ctGeofenceAPI.handleGeoFences(null)

        verify(executorService, never()).submit(any(Runnable::class.java))
    }

    @Test
    fun testHandleGeoFencesTC4() {
        // when location access permission and background location permission is granted
        `when`(
            Utils.hasPermission(
                application, Manifest.permission.ACCESS_FINE_LOCATION
            )
        ).thenReturn(true)
        `when`(Utils.hasBackgroundLocationPermission(application)).thenReturn(true)

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        ctGeofenceAPI.handleGeoFences(GeofenceJSON.geofence)

        verify(executorService).submit(any(Runnable::class.java))
    }

    @Test
    fun testInitBackgroundLocationUpdatesTC1() {
        // when location access permission is denied
        `when`(
            Utils.hasPermission(
                application, Manifest.permission.ACCESS_FINE_LOCATION
            )
        ).thenReturn(false)

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        ctGeofenceAPI.initBackgroundLocationUpdates()

        verify(executorService, never()).submit(any(Runnable::class.java))
    }

    @Test(expected = IllegalStateException::class)
    fun testInitBackgroundLocationUpdatesTC2() {
        // when geofence init is not called
        `when`(
            Utils.hasPermission(
                application, Manifest.permission.ACCESS_FINE_LOCATION
            )
        ).thenReturn(true)

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        ctGeofenceAPI.initBackgroundLocationUpdates()

        verify(executorService, never()).submit(any(Runnable::class.java))
    }

    @Test
    fun testInitBackgroundLocationUpdatesTC3() {
        // when geofence sdk initialized and location access permission is granted
        `when`(
            Utils.hasPermission(
                application, Manifest.permission.ACCESS_FINE_LOCATION
            )
        ).thenReturn(true)

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        ctGeofenceAPI.init(null, cleverTapAPI)

        verify(executorService).submit(any(Runnable::class.java))
    }

    @Test
    fun testInitTC1() {
        // when location adapter is null
        ctLocationFactoryMockedStatic.`when`<CTLocationAdapter> {
            CTLocationFactory.createLocationAdapter(application)
        }.thenReturn(null)
        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)

        ctGeofenceAPI.init(null, cleverTapAPI)
        verifyNoMoreInteractions(cleverTapAPI)
    }

    @Test
    fun testInitTC2() {
        // when geofence adapter is null
        ctGeofenceFactoryMockedStatic.`when`<CTGeofenceAdapter> {
            CTGeofenceFactory.createGeofenceAdapter(application)
        }.thenReturn(null)

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)

        ctGeofenceAPI.init(null, cleverTapAPI)
        verifyNoMoreInteractions(cleverTapAPI)
    }

    @Test
    fun testProcessTriggeredLocationTC1() {
        // when delta t and delta d both is satisfied

        GeofenceStorageHelper.putDouble(
            application, CTGeofenceConstants.KEY_LATITUDE, DEFAULT_LATITUDE
        )
        GeofenceStorageHelper.putDouble(
            application, CTGeofenceConstants.KEY_LONGITUDE, DEFAULT_LONGITUDE
        )
        GeofenceStorageHelper.putLong(
            application,
            CTGeofenceConstants.KEY_LAST_LOCATION_EP,
            System.currentTimeMillis() - 2400000
        )// move to past by 40 minutes

        val location = Location("")
        val lat = 19.23041616
        val lng = 72.82488101
        location.latitude = lat
        location.longitude = lng

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        ctGeofenceAPI.init(null, cleverTapAPI)
        ctGeofenceAPI.processTriggeredLocation(location)

        val actualLat = GeofenceStorageHelper.getDouble(
            application, CTGeofenceConstants.KEY_LATITUDE, 0.0
        )
        val actualLong = GeofenceStorageHelper.getDouble(
            application, CTGeofenceConstants.KEY_LONGITUDE, 0.0
        )

        assertEquals(lat, actualLat, 0.0)
        assertEquals(lng, actualLong, 0.0)

        verify(cleverTapAPI).setLocationForGeofences(any(Location::class.java), anyInt())
    }

    @Test
    fun testProcessTriggeredLocationTC2() {
        // when delta t is satisfied

        val lastPingedLat = 19.23051746
        val lastPingedLng = 72.82425874

        GeofenceStorageHelper.putDouble(
            application, CTGeofenceConstants.KEY_LATITUDE, lastPingedLat
        )
        GeofenceStorageHelper.putDouble(
            application, CTGeofenceConstants.KEY_LONGITUDE, lastPingedLng
        )
        GeofenceStorageHelper.putLong(
            application,
            CTGeofenceConstants.KEY_LAST_LOCATION_EP,
            System.currentTimeMillis() - 2400000
        )// move to past by 40 minutes

        val location = Location("")
        location.latitude = 19.23041616
        location.longitude = 72.82488101

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        ctGeofenceAPI.init(null, cleverTapAPI)
        ctGeofenceAPI.processTriggeredLocation(location)

        val actualLat = GeofenceStorageHelper.getDouble(
            application, CTGeofenceConstants.KEY_LATITUDE, 0.0
        )
        val actualLong = GeofenceStorageHelper.getDouble(
            application, CTGeofenceConstants.KEY_LONGITUDE, 0.0
        )

        assertEquals(lastPingedLat, actualLat, 0.0)
        assertEquals(lastPingedLng, actualLong, 0.0)

        verify(cleverTapAPI, never()).setLocationForGeofences(any(), anyInt())
    }

    @Test
    fun testProcessTriggeredLocationTC3() {
        // when delta d is satisfied

        GeofenceStorageHelper.putDouble(
            application, CTGeofenceConstants.KEY_LATITUDE, DEFAULT_LATITUDE
        )
        GeofenceStorageHelper.putDouble(
            application, CTGeofenceConstants.KEY_LONGITUDE, DEFAULT_LONGITUDE
        )
        GeofenceStorageHelper.putLong(
            application,
            CTGeofenceConstants.KEY_LAST_LOCATION_EP,
            System.currentTimeMillis() - 1500000
        )// move to pas by 25 minutes

        val location = Location("")
        location.latitude = 19.23041616
        location.longitude = 72.82488101

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        ctGeofenceAPI.init(null, cleverTapAPI)
        ctGeofenceAPI.processTriggeredLocation(location)

        val actualLat = GeofenceStorageHelper.getDouble(
            application, CTGeofenceConstants.KEY_LATITUDE, 0.0
        )
        val actualLong = GeofenceStorageHelper.getDouble(
            application, CTGeofenceConstants.KEY_LONGITUDE, 0.0
        )

        assertEquals(DEFAULT_LATITUDE, actualLat, 0.0)
        assertEquals(DEFAULT_LONGITUDE, actualLong, 0.0)
        verify(cleverTapAPI, never()).setLocationForGeofences(any(), anyInt())
    }

    @Test
    fun testProcessTriggeredLocationTC4() {
        // when delta t is not satisfied and delta d is not satisfied

        val lastPingedLat = 19.23051746
        val lastPingedLng = 72.82425874

        GeofenceStorageHelper.putDouble(
            application, CTGeofenceConstants.KEY_LATITUDE, lastPingedLat
        )
        GeofenceStorageHelper.putDouble(
            application, CTGeofenceConstants.KEY_LONGITUDE, lastPingedLng
        )
        GeofenceStorageHelper.putLong(
            application,
            CTGeofenceConstants.KEY_LAST_LOCATION_EP,
            System.currentTimeMillis() - 1500000
        )// move to pas by 25 minutes

        val location = Location("")
        location.latitude = 19.23041616
        location.longitude = 72.82488101

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        ctGeofenceAPI.init(null, cleverTapAPI)
        ctGeofenceAPI.processTriggeredLocation(location)

        val actualLat = GeofenceStorageHelper.getDouble(
            application, CTGeofenceConstants.KEY_LATITUDE, 0.0
        )
        val actualLong = GeofenceStorageHelper.getDouble(
            application, CTGeofenceConstants.KEY_LONGITUDE, 0.0
        )

        assertEquals(lastPingedLat, actualLat, 0.0)
        assertEquals(lastPingedLng, actualLong, 0.0)
        verify(cleverTapAPI, never()).setLocationForGeofences(any(), anyInt())
    }

    @Test
    fun testSetCtGeofenceEventsListener() {
        val listener = object : CTGeofenceEventsListener {
            override fun onGeofenceEnteredEvent(geofenceEnteredEventProperties: JSONObject) {

            }

            override fun onGeofenceExitedEvent(geofenceExitedEventProperties: JSONObject) {

            }
        }

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        ctGeofenceAPI.setCtGeofenceEventsListener(listener)
        assertSame(listener, ctGeofenceAPI.ctGeofenceEventsListener)
    }

    @Test
    fun testSetCtLocationUpdatesListener() {
        val listener = CTLocationUpdatesListener {}

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        ctGeofenceAPI.setCtLocationUpdatesListener(listener)
        assertSame(listener, ctGeofenceAPI.ctLocationUpdatesListener)
    }

    @Test
    fun testTriggerLocationTC1() {
        // when location access permission is denied
        `when`(
            Utils.hasPermission(
                application, Manifest.permission.ACCESS_FINE_LOCATION
            )
        ).thenReturn(false)

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        ctGeofenceAPI.triggerLocation()

        verify(executorService, never()).submit(any(Runnable::class.java))
    }

    @Test
    fun testTriggerLocationTC2() {
        // when geofence init is not called
        `when`(
            Utils.hasPermission(
                application, Manifest.permission.ACCESS_FINE_LOCATION
            )
        ).thenReturn(true)

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        ctGeofenceAPI.triggerLocation()

        verify(executorService, never()).submit(any(Runnable::class.java))
    }

    @Test
    fun testTriggerLocationTC3() {
        // when geofence sdk initialized and location access permission is granted
        `when`(
            Utils.hasPermission(
                application, Manifest.permission.ACCESS_FINE_LOCATION
            )
        ).thenReturn(true)

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        ctGeofenceAPI.init(null, cleverTapAPI)

        ctGeofenceAPI.triggerLocation()

        verify(executorService, times(2)).submit(any())
    }

}
