package com.clevertap.android.geofence.interfaces;

import android.location.Location;

/**
 * Callback interface to get Last Known Location from OS.
 * <b>This is for internal usage only.</b> Applications can use {@link CTLocationUpdatesListener} for foreground
 * as well as background location updates.
 */
public interface CTLocationCallback {

    /**
     * This method will be invoked when a location will be available from OS
     *
     * @param location an instance of {@link Location}
     */
    void onLocationComplete(Location location);

}
