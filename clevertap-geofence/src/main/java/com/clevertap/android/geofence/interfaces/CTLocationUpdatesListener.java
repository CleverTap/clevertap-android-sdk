package com.clevertap.android.geofence.interfaces;

import android.location.Location;

/**
 * Callback interface to get foreground as well as background location updates from OS.
 * <p>
 * Kept in Java on purpose (SDK-6395): Kotlin sees the parameter as a platform type, so apps can
 * override it with a nullable or non-null type. Do not convert this file to Kotlin.
 */
public interface CTLocationUpdatesListener {

    /**
     * This method will be invoked on main thread when a location will be available from OS
     *
     * @param location an instance of {@link Location}
     */
    void onLocationUpdates(Location location);
}
