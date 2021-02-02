package com.clevertap.android.sdk

import android.location.Location
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.runner.*
import org.mockito.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.Future
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class LocationManagerTest : BaseTestCase() {

    private lateinit var coreMetaData: CoreMetaData
    private lateinit var locationManager: LocationManager
    private lateinit var eventQueueManager: BaseEventQueueManager
    private lateinit var location: Location

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        coreMetaData = CoreMetaData()
        eventQueueManager = Mockito.mock(EventQueueManager::class.java)
        locationManager = spy(LocationManager(application, cleverTapInstanceConfig, coreMetaData, eventQueueManager))
        location = Location("")
        location.latitude = 17.355
        location.longitude = 7.355
    }

    @Test
    fun test_setLocation_returns_null_when_Location_is_null() {
        val future = locationManager._setLocation(null)

        assertNull(future)
    }

    @Test
    fun test_setLocation_returns_null_when_app_in_background_and_Location_is_not_for_geofence() {
        CoreMetaData.setAppForeground(false)
        coreMetaData.isLocationForGeofence = false

        val future = locationManager._setLocation(location)

        assertNull(future)
    }

    @Test
    fun test_setLocation_returns_null_when_Location_is_for_geofence_and_last_location_geofence_ping_is_less_than_10_secs() {
        `when`(locationManager.now).thenReturn(100)
        CoreMetaData.setAppForeground(true)
        coreMetaData.isLocationForGeofence = true
        locationManager.lastLocationPingTimeForGeofence = 100

        val future = locationManager._setLocation(location)

        assertNull(future)
        assertEquals(locationManager.lastLocationPingTimeForGeofence, 100)
    }

    @Test
    fun test_setLocation_returns_future_when_Location_is_for_geofence_and_last_location_geofence_ping_is_greater_than_10_secs() {
        val future = mock(Future::class.java)

        `when`(locationManager.now).thenReturn(150)
        `when`(eventQueueManager.queueEvent(any(), any(), anyInt()))
            .thenReturn(future)

        CoreMetaData.setAppForeground(true)
        coreMetaData.isLocationForGeofence = true
        locationManager.lastLocationPingTimeForGeofence = 100

        val futureActual = locationManager._setLocation(location)

        verify(eventQueueManager).queueEvent(any(), any(), anyInt())
        assertNotNull(futureActual)
        assertEquals(locationManager.lastLocationPingTimeForGeofence, 150)
    }

    @Test
    fun test_setLocation_returns_null_when_Location_is_not_for_geofence_and_last_location_ping_is_less_than_10_secs() {
        `when`(locationManager.now).thenReturn(100)
        CoreMetaData.setAppForeground(true)
        coreMetaData.isLocationForGeofence = false
        locationManager.lastLocationPingTime = 100

        val future = locationManager._setLocation(location)

        assertNull(future)
        assertEquals(locationManager.lastLocationPingTime, 100)
    }

    @Test
    fun test_setLocation_returns_future_when_Location_is_not_for_geofence_and_last_location_ping_is_greater_than_10_secs() {
        val future = mock(Future::class.java)

        `when`(locationManager.now).thenReturn(150)
        `when`(eventQueueManager.queueEvent(any(), any(), anyInt()))
            .thenReturn(future)

        CoreMetaData.setAppForeground(true)
        coreMetaData.isLocationForGeofence = false
        locationManager.lastLocationPingTime = 100

        val futureActual = locationManager._setLocation(location)

        verify(eventQueueManager).queueEvent(any(), any(), anyInt())
        assertNotNull(futureActual)
        assertEquals(locationManager.lastLocationPingTime, 150)
    }
}
