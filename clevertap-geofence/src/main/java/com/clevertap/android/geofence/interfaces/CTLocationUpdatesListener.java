package com.clevertap.android.geofence.interfaces;

import android.location.Location;

/**
 * Callback interface to get foreground as well as background location updates from OS.
 */
public interface CTLocationUpdatesListener {

    /**
     * This method will be invoked on main thread when a location will be available from OS
     *
     * @param location an instance of {@link Location}
     */
    void onLocationUpdates(Location location);
}
