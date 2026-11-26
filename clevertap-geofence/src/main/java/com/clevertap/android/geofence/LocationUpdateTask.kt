package com.clevertap.android.geofence

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_NO_CREATE
import android.content.Context
import androidx.annotation.WorkerThread
import com.clevertap.android.geofence.interfaces.CTGeofenceTask
import com.clevertap.android.geofence.interfaces.CTGeofenceTask.OnCompleteListener
import com.clevertap.android.geofence.interfaces.CTLocationAdapter
import androidx.annotation.RestrictTo

/**
 * A task of type [CTGeofenceTask] responsible for requesting or removing background
 * location updates by comparing [CTGeofenceSettings] and [PendingIntent]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class LocationUpdateTask(context: Context) : CTGeofenceTask {

    private val context: Context = context.applicationContext

    private var ctGeofenceSettings: CTGeofenceSettings? = CTGeofenceAPI.getInstance(this.context).geofenceSettings

    private val ctLocationAdapter: CTLocationAdapter? = CTGeofenceAPI.getInstance(this.context).ctLocationAdapter

    private var onCompleteListener: OnCompleteListener? = null

    /**
     * Writes new [CTGeofenceSettings] to file. Requests/Removes Location updates based on change
     * in config settings and avoids duplicate request of location updates if pending intent
     * already exists in the system and is active
     */
    @WorkerThread
    override fun execute() {
        if (ctLocationAdapter == null) {
            return
        }

        if (ctGeofenceSettings == null) {
            ctGeofenceSettings = CTGeofenceAPI.getInstance(context).initDefaultConfig()
        }

        CTGeofenceAPI.getLogger().debug(
            CTGeofenceAPI.GEOFENCE_LOG_TAG,
            "Executing LocationUpdateTask..."
        )

        // FLAG_NO_CREATE will tell us if pending intent already exists and is active
        val locationPendingIntent = PendingIntentFactory.getPendingIntent(
            context,
            PendingIntentFactory.PENDING_INTENT_LOCATION, FLAG_NO_CREATE
        )

        // if background location disabled and if location update request is already registered then remove it
        if (!this.ctGeofenceSettings!!.isBackgroundLocationUpdatesEnabled && locationPendingIntent != null) {
            ctLocationAdapter.removeLocationUpdates(locationPendingIntent)
        } else if (isRequestLocation(locationPendingIntent)) {
            // if background location enabled and if location update request is not already registered
            // or there is change in accuracy or fetch mode settings then request location updates
            ctLocationAdapter.requestLocationUpdates()
        } else {
            CTGeofenceAPI.getLogger().verbose(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Dropping duplicate location update request"
            )
        }

        // write new settings to file
        Utils.writeSettingsToFile(context, ctGeofenceSettings!!)

        if (onCompleteListener != null) {
            onCompleteListener!!.onComplete()
        }

        CTGeofenceAPI.getLogger().debug(
            CTGeofenceAPI.GEOFENCE_LOG_TAG,
            "Finished executing LocationUpdateTask"
        )
    }

    override fun setOnCompleteListener(onCompleteListener: OnCompleteListener) {
        this.onCompleteListener = onCompleteListener
    }

    /**
     * Helper method for comparing old [CTGeofenceSettings] stored in file with current one to
     * determine if location update request is required.
     *
     * @param locationPendingIntent instance of [PendingIntent] of type
     * [PendingIntentFactory.PENDING_INTENT_GEOFENCE] or
     * [PendingIntentFactory.PENDING_INTENT_LOCATION]
     * @return true if location update request is required else false
     */
    private fun isRequestLocation(locationPendingIntent: PendingIntent?): Boolean {
        var lastAccuracy = -1
        var lastFetchMode = -1
        var lastInterval: Long = -1
        var lastFastestInterval: Long = -1
        var lastDisplacement = -1f

        // ctGeofenceSettings won't be null
        val ctGeofenceSettings = ctGeofenceSettings!!
        val currentAccuracy = ctGeofenceSettings.locationAccuracy
        val currentFetchMode = ctGeofenceSettings.locationFetchMode
        val currentInterval = ctGeofenceSettings.interval
        val currentFastestInterval = ctGeofenceSettings.fastestInterval
        val currentDisplacement = ctGeofenceSettings.smallestDisplacement

        // read settings from file
        val lastGeofenceSettings = Utils.readSettingsFromFile(context)
        if (lastGeofenceSettings != null) {
            lastAccuracy = lastGeofenceSettings.locationAccuracy
            lastFetchMode = lastGeofenceSettings.locationFetchMode
            lastInterval = lastGeofenceSettings.interval
            lastFastestInterval = lastGeofenceSettings.fastestInterval
            lastDisplacement = lastGeofenceSettings.smallestDisplacement
        }

        val isCurrentLocationFetchModeChanged =
            currentFetchMode == CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC.toInt() &&
                    (currentAccuracy != lastAccuracy || currentInterval != lastInterval
                            || currentFastestInterval != lastFastestInterval
                            || currentDisplacement != lastDisplacement)

        val isLastLocationFetchModeChanged =
            currentFetchMode == CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC.toInt() &&
                    currentInterval != lastInterval

        return ctGeofenceSettings.isBackgroundLocationUpdatesEnabled &&
                (locationPendingIntent == null || isCurrentLocationFetchModeChanged
                        || isLastLocationFetchModeChanged || currentFetchMode != lastFetchMode)
    }
}
