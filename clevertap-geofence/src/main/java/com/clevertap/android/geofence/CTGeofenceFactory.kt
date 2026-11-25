package com.clevertap.android.geofence

import android.content.Context
import com.clevertap.android.geofence.interfaces.CTGeofenceAdapter
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * A Factory to create an instance of [CTGeofenceAdapter]
 */
internal object CTGeofenceFactory {

    /**
     * Creates an instance of [CTGeofenceAdapter], if FusedLocationApi dependency and
     * Play service APK in device is available
     *
     * @param context application [Context]
     * @return an instance of [CTGeofenceAdapter]
     * @throws IllegalStateException if play-services-location dependency is missing or Play service APK
     * error is reported by GoogleApi.
     */
    @JvmStatic
    fun createGeofenceAdapter(context: Context): CTGeofenceAdapter? {
        val googlePlayServicesAvailable = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context)

        if (Utils.isFusedLocationApiDependencyAvailable()) {
            if (googlePlayServicesAvailable == ConnectionResult.SUCCESS) {
                return GoogleGeofenceAdapter(context.applicationContext)
            } else {
                val errorString = GoogleApiAvailability.getInstance().getErrorString(googlePlayServicesAvailable)
                throw IllegalStateException("Play service APK error :: $errorString")
            }
        } else {
            throw IllegalStateException("play-services-location dependency is missing")
        }
    }
}
