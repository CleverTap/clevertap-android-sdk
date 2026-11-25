package com.clevertap.android.geofence.interfaces

import android.location.Location

/**
 * Callback interface to get Last Known Location from OS.
 * **This is for internal usage only.** Applications can use [CTLocationUpdatesListener] for foreground
 * as well as background location updates.
 */
internal fun interface CTLocationCallback {

    /**
     * This method will be invoked when a location will be available from OS
     *
     * @param location an instance of [Location]
     */
    fun onLocationComplete(location: Location?)
}
