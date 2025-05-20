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
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.concurrent.ExecutorService

class CTGeofenceAPITest : BaseTestCase() {

    private lateinit var cleverTapAPI: CleverTapAPI
    private lateinit var executorService: ExecutorService
    private lateinit var geofenceAdapter: CTGeofenceAdapter
    private lateinit var locationAdapter: CTLocationAdapter

    override fun setUp() {
        super.setUp()

        cleverTapAPI = mockk(relaxed = true)
        executorService = mockk(relaxed = true)
        geofenceAdapter = mockk(relaxed = true)
        locationAdapter = mockk(relaxed = true)

        mockkStatic(CTLocationFactory::class)
        mockkStatic(CTGeofenceFactory::class)
        mockkStatic(Utils::class)

        every { CTLocationFactory.createLocationAdapter(application) } returns locationAdapter
        every { CTGeofenceFactory.createGeofenceAdapter(application) } returns geofenceAdapter
    }

    override fun cleanUp() {
        super.cleanUp()
        val instance = CTGeofenceAPI.getInstance(application)
        val field = CTGeofenceAPI::class.java.getDeclaredField("ctGeofenceAPI")
        field.isAccessible = true
        field.set(instance, null)

        val taskManagerInstance = CTGeofenceTaskManager.getInstance()
        val fieldTaskManager = CTGeofenceTaskManager::class.java.getDeclaredField("taskManager")
        fieldTaskManager.isAccessible = true
        fieldTaskManager.set(taskManagerInstance, null)

        unmockkStatic(CTLocationFactory::class)
        unmockkStatic(CTGeofenceFactory::class)
        unmockkStatic(Utils::class)
    }

    @Test
    fun testDeactivate() {
        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        mockkStatic(FileUtils::class) {
            ctGeofenceAPI.deactivate()
            val runnableSlot = slot<Runnable>()
            verify { executorService.submit(capture(runnableSlot)) }

            val geofenceMonitoring = PendingIntentFactory.getPendingIntent(
                application, PendingIntentFactory.PENDING_INTENT_GEOFENCE, FLAG_UPDATE_CURRENT
            )
            val locationUpdates = PendingIntentFactory.getPendingIntent(
                application, PendingIntentFactory.PENDING_INTENT_LOCATION, FLAG_UPDATE_CURRENT
            )

            runnableSlot.captured.run()

            verify { geofenceAdapter.stopGeofenceMonitoring(geofenceMonitoring) }
            verify { locationAdapter.removeLocationUpdates(locationUpdates) }

            val cachedDirName = FileUtils.getCachedDirName(application)
            verify { FileUtils.deleteDirectory(any(), cachedDirName) }

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
        every {
            Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns false

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        ctGeofenceAPI.handleGeoFences(GeofenceJSON.geofence)

        verify(exactly = 0) { executorService.submit(any()) }
    }

    @Test
    fun testHandleGeoFencesTC2() {
        // when background location permission is not granted
        every {
            Utils.hasPermission(
                application, Manifest.permission.ACCESS_FINE_LOCATION
            )
        } returns true
        every { Utils.hasBackgroundLocationPermission(application) } returns false

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        ctGeofenceAPI.handleGeoFences(GeofenceJSON.geofence)

        verify(exactly = 0) { executorService.submit(any()) }
    }

    @Test
    fun testHandleGeoFencesTC3() {
        // when geofence list is null
        every {
            Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns true
        every { Utils.hasBackgroundLocationPermission(application) } returns true

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        ctGeofenceAPI.handleGeoFences(null)

        verify(exactly = 0) { executorService.submit(any()) }
    }

    @Test
    fun testHandleGeoFencesTC4() {
        // when location access permission and background location permission is granted
        every {
            Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns true
        every { Utils.hasBackgroundLocationPermission(application) } returns true

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        ctGeofenceAPI.handleGeoFences(GeofenceJSON.geofence)

        verify { executorService.submit(any()) }
    }

    @Test
    fun testInitBackgroundLocationUpdatesTC1() {
        // when location access permission is denied
        every {
            Utils.hasPermission(
                application, Manifest.permission.ACCESS_FINE_LOCATION
            )
        } returns false

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        ctGeofenceAPI.initBackgroundLocationUpdates()

        verify(exactly = 0) { executorService.submit(any()) }
    }

    @Test(expected = IllegalStateException::class)
    fun testInitBackgroundLocationUpdatesTC2() {
        // when geofence init is not called
        every {
            Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns true

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        ctGeofenceAPI.initBackgroundLocationUpdates()

        verify(exactly = 0) { executorService.submit(any()) }
    }

    @Test
    fun testInitBackgroundLocationUpdatesTC3() {
        // when geofence sdk initialized and location access permission is granted
        every {
            Utils.hasPermission(application, Manifest.permission.ACCESS_FINE_LOCATION)
        } returns true

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        ctGeofenceAPI.init(null, cleverTapAPI)

        verify { executorService.submit(any()) }
    }

    @Test
    fun testInitTC1() {
        // when location adapter is null
        every {
            CTLocationFactory.createLocationAdapter(application)
        } returns null
        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)

        ctGeofenceAPI.init(null, cleverTapAPI)
        confirmVerified(cleverTapAPI)
    }

    @Test
    fun testInitTC2() {
        // when geofence adapter is null
        every {
            CTGeofenceFactory.createGeofenceAdapter(application)
        } returns null

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)

        ctGeofenceAPI.init(null, cleverTapAPI)
        confirmVerified(cleverTapAPI)
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

        verify { cleverTapAPI.setLocationForGeofences(any(), any()) }
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

        verify(exactly = 0) { cleverTapAPI.setLocationForGeofences(any(), any()) }
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
        verify(exactly = 0) { cleverTapAPI.setLocationForGeofences(any(), any()) }
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
        verify(exactly = 0) { cleverTapAPI.setLocationForGeofences(any(), any()) }
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
        every {
            Utils.hasPermission(
                application, Manifest.permission.ACCESS_FINE_LOCATION
            )
        } returns false

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        ctGeofenceAPI.triggerLocation()

        verify(exactly = 0) { executorService.submit(any()) }
    }

    @Test
    fun testTriggerLocationTC2() {
        // when geofence init is not called
        every {
            Utils.hasPermission(
                application, Manifest.permission.ACCESS_FINE_LOCATION
            )
        } returns true

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        ctGeofenceAPI.triggerLocation()

        verify(exactly = 0) { executorService.submit(any()) }
    }

    @Test
    fun testTriggerLocationTC3() {
        // when geofence sdk initialized and location access permission is granted
        every {
            Utils.hasPermission(
                application, Manifest.permission.ACCESS_FINE_LOCATION
            )
        } returns true

        val ctGeofenceAPI = CTGeofenceAPI.getInstance(application)
        CTGeofenceTaskManager.getInstance().setExecutorService(executorService)
        ctGeofenceAPI.init(null, cleverTapAPI)

        ctGeofenceAPI.triggerLocation()

        verify(exactly = 2) { executorService.submit(any()) }
    }
}
