package com.clevertap.android.geofence.interfaces;

import android.app.PendingIntent;

/**
 * Interface that defines API-type(Google APIs/Android platform APIs) class that can be used to add
 * and remove background location updates to and from OS respectively
 */
public interface CTLocationAdapter {

    /**
     * Fetches Last Known Location from OS and delivers it to caller through given {@link CTLocationCallback}
     *
     * @param callback to get last location
     */
    void getLastLocation(CTLocationCallback callback);

    /**
     * Unregisters background location updates associated with the given {@link PendingIntent} from OS
     */
    void removeLocationUpdates(PendingIntent pendingIntent);

    /**
     * Registers background location updates to OS
     */
    void requestLocationUpdates();

}
