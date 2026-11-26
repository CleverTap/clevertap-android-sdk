package com.clevertap.android.geofence.interfaces

import android.location.Location
import androidx.annotation.RestrictTo

/**
 * Callback interface to get Last Known Location from OS.
 * **This is for internal usage only.** Applications can use [CTLocationUpdatesListener] for foreground
 * as well as background location updates.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun interface CTLocationCallback {

    /**
     * This method will be invoked when a location will be available from OS
     *
     * @param location an instance of [Location]
     */
    fun onLocationComplete(location: Location?)
}
