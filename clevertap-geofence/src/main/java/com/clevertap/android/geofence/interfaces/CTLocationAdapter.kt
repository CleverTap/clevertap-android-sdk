package com.clevertap.android.geofence.interfaces

import android.app.PendingIntent

/**
 * Interface that defines API-type(Google APIs/Android platform APIs) class that can be used to add
 * and remove background location updates to and from OS respectively
 */
internal interface CTLocationAdapter {

    /**
     * Fetches Last Known Location from OS and delivers it to caller through given [CTLocationCallback]
     *
     * @param callback to get last location
     */
    fun getLastLocation(callback: CTLocationCallback)

    /**
     * Unregisters background location updates associated with the given [PendingIntent] from OS
     */
    fun removeLocationUpdates(pendingIntent: PendingIntent?)

    /**
     * Registers background location updates to OS
     */
    fun requestLocationUpdates()
}
