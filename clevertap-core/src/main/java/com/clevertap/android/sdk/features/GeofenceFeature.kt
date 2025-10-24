package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.BaseLocationManager
import com.clevertap.android.sdk.GeofenceCallback

/**
 * Geofence feature
 * Manages geofencing operations and location tracking
 */
internal data class GeofenceFeature(
    val locationManager: BaseLocationManager
) {
    var geofenceCallback: GeofenceCallback? = null
}
