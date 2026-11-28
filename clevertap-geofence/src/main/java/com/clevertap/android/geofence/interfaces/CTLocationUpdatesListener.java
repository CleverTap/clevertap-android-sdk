package com.clevertap.android.geofence.interfaces;

import android.location.Location;

import androidx.annotation.Nullable;

/**
 * Callback interface to get foreground as well as background location updates from OS.
 * <p>
 * Kept in Java on purpose (SDK-6395) so the 8.x bytecode shape stays the same. The parameter
 * carries androidx nullability annotations that match what the SDK passes (SDK-6396), so a Kotlin
 * override must declare the same nullability. Do not convert this file to Kotlin.
 */
public interface CTLocationUpdatesListener {

    /**
     * This method will be invoked on main thread when a location will be available from OS
     *
     * @param location an instance of {@link Location}
     */
    void onLocationUpdates(@Nullable Location location);
}
