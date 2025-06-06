package com.clevertap.android.geofence

import com.clevertap.android.geofence.CTGeofenceSettings.ACCURACY_HIGH
import com.clevertap.android.geofence.CTGeofenceSettings.ACCURACY_MEDIUM
import com.clevertap.android.geofence.CTGeofenceSettings.DEFAULT_GEO_MONITOR_COUNT
import com.clevertap.android.geofence.CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC
import com.clevertap.android.geofence.CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CTGeofenceSettingsTest : BaseTestCase() {

    @Test
    fun testCustomSettings() {
        // when interval, fastestInterval and displacement are valid
        val customSettings = CTGeofenceSettings.Builder().enableBackgroundLocationUpdates(false)
            .setLogLevel(Logger.INFO).setLocationAccuracy(ACCURACY_MEDIUM)
            .setLocationFetchMode(FETCH_CURRENT_LOCATION_PERIODIC).setGeofenceMonitoringCount(98)
            .setInterval(2000000).setFastestInterval(1900000).setSmallestDisplacement(780f).build()

        assertFalse(customSettings.isBackgroundLocationUpdatesEnabled)
        assertEquals(ACCURACY_MEDIUM, customSettings.locationAccuracy.toByte())
        assertEquals(
            FETCH_CURRENT_LOCATION_PERIODIC, customSettings.locationFetchMode.toByte()
        )
        assertEquals(Logger.INFO, customSettings.logLevel)
        assertEquals(98, customSettings.geofenceMonitoringCount)
        assertNull(customSettings.id)
        assertEquals(2000000, customSettings.interval)
        assertEquals(1900000, customSettings.fastestInterval)
        assertEquals(780f, customSettings.smallestDisplacement, 0f)

        // when interval, fastestInterval and displacement are invalid

        val inValidSettings =
            CTGeofenceSettings.Builder().setInterval(120000).setFastestInterval(120000)
                .setSmallestDisplacement(100f).build()

        assertEquals(1800000, inValidSettings.interval)
        assertEquals(1800000, inValidSettings.fastestInterval)
        assertEquals(200f, inValidSettings.smallestDisplacement, 0f)
    }

    @Test
    fun testDefaultSettings() {

        val defaultSettings = CTGeofenceSettings.Builder().build()

        assertTrue(defaultSettings.isBackgroundLocationUpdatesEnabled)
        assertEquals(ACCURACY_HIGH, defaultSettings.locationAccuracy.toByte())
        assertEquals(FETCH_LAST_LOCATION_PERIODIC, defaultSettings.locationFetchMode.toByte())
        assertEquals(Logger.DEBUG, defaultSettings.logLevel)
        assertEquals(DEFAULT_GEO_MONITOR_COUNT, defaultSettings.geofenceMonitoringCount)
        assertNull(defaultSettings.id)
        assertEquals(GoogleLocationAdapter.INTERVAL_IN_MILLIS, defaultSettings.interval)
        assertEquals(
            GoogleLocationAdapter.INTERVAL_FASTEST_IN_MILLIS, defaultSettings.fastestInterval
        )
        assertEquals(
            GoogleLocationAdapter.SMALLEST_DISPLACEMENT_IN_METERS,
            defaultSettings.smallestDisplacement,
            0f
        )
    }
}
