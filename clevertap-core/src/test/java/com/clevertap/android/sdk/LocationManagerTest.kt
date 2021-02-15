package com.clevertap.android.sdk

import android.content.Context
import android.location.Location
import com.clevertap.android.sdk.events.BaseEventQueueManager
import com.clevertap.android.sdk.events.EventQueueManager
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.runner.*
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
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
        eventQueueManager = mock(EventQueueManager::class.java)
        locationManager = spy(LocationManager(application, cleverTapInstanceConfig, coreMetaData, eventQueueManager))
        location = Location("").apply {
            latitude = 17.355
            longitude = 7.355
            accuracy = 50f
        }
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

    @Test
    fun test_getLocation_returns_null_when_location_manager_dont_have_providers_enabled() {
        val actualLocation = locationManager._getLocation()
        assertNull(actualLocation)
    }

    @Test
    fun test_getLocation_returns_location_when_location_manager_has_any_providers_enabled() {
        val systemService = application.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val shadowOf = shadowOf(systemService)
        shadowOf.setProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER, true)
        shadowOf.setLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER, location)

        val actualLocation = locationManager._getLocation()

        assertEquals(location, actualLocation)
    }

    @Test
    fun test_getLocation_returns_null_when_location_provider_does_not_have_location() {
        val systemService = application.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val shadowOf = shadowOf(systemService)
        shadowOf.setProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER, true)
        shadowOf.setLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER, null)

        val actualLocation = locationManager._getLocation()

        assertNull(actualLocation)
    }

    @Test
    fun test_getLocation_returns_location_with_best_accuracy() {
        val systemService = application.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val shadowOf = shadowOf(systemService)
        shadowOf.setProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER, true)
        shadowOf.setProviderEnabled(android.location.LocationManager.GPS_PROVIDER, true)
        shadowOf.setLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER, location)

        val gpsLocation = Location("").apply {
            latitude = 17.355
            longitude = 7.355
            accuracy = 10f
        }
        shadowOf.setLastKnownLocation(android.location.LocationManager.GPS_PROVIDER, gpsLocation)

        val actualLocation = locationManager._getLocation()

        assertEquals(gpsLocation, actualLocation)
    }
}
