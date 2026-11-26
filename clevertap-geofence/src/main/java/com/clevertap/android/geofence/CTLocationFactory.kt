package com.clevertap.android.geofence

import android.content.Context
import com.clevertap.android.geofence.interfaces.CTLocationAdapter
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import androidx.annotation.RestrictTo

/**
 * A Factory to create an instance of [CTLocationAdapter]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object CTLocationFactory {

    /**
     * Creates an instance of [CTLocationAdapter], if FusedLocationApi dependency and
     * Play service APK in device is available
     *
     * @param context application [Context]
     * @return an instance of [CTLocationAdapter]
     * @throws IllegalStateException if play-services-location dependency is missing or Play service APK
     * error is reported by GoogleApi.
     */
    @JvmStatic
    fun createLocationAdapter(context: Context): CTLocationAdapter? {
        val googlePlayServicesAvailable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)

        if (Utils.isFusedLocationApiDependencyAvailable()) {
            CTGeofenceAPI.getLogger().info(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "FusedLocationApi dependency is available"
            )

            if (googlePlayServicesAvailable == ConnectionResult.SUCCESS) {
                CTGeofenceAPI.getLogger().info(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Play service APK is available"
                )
                return GoogleLocationAdapter(context.applicationContext)
            } else {
                val errorString = GoogleApiAvailability.getInstance().getErrorString(googlePlayServicesAvailable)
                throw IllegalStateException("Play service APK error :: $errorString")
            }
        } else {
            throw IllegalStateException("play-services-location dependency is missing")
        }
    }
}
