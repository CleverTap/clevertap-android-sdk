package com.clevertap.android.geofence;

import android.content.Context;
import androidx.annotation.NonNull;
import com.clevertap.android.geofence.interfaces.CTGeofenceAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

/**
 * A Factory to create an instance of {@link CTGeofenceAdapter}
 */
class CTGeofenceFactory {

    /**
     * Creates an instance of {@link CTGeofenceAdapter}, if FusedLocationApi dependency and
     * Play service APK in device is available
     *
     * @param context application {@link Context}
     * @return an instance of {@link CTGeofenceAdapter}
     * @throws IllegalStateException if play-services-location dependency is missing or Play service APK
     *                               error is reported by GoogleApi.
     */
    static CTGeofenceAdapter createGeofenceAdapter(@NonNull Context context) {

        int googlePlayServicesAvailable = GoogleApiAvailability.getInstance().
                isGooglePlayServicesAvailable(context);

        if (Utils.isFusedLocationApiDependencyAvailable()) {

            if (googlePlayServicesAvailable == ConnectionResult.SUCCESS) {
                return new GoogleGeofenceAdapter(context.getApplicationContext());
            } else {
                String errorString = GoogleApiAvailability.getInstance().getErrorString(googlePlayServicesAvailable);
                throw new IllegalStateException("Play service APK error :: " + errorString);
            }
        } else {
            throw new IllegalStateException("play-services-location dependency is missing");
        }
    }
}
