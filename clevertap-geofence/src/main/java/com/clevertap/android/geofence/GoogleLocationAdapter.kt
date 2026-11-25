package com.clevertap.android.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_NO_CREATE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.location.Location
import androidx.annotation.WorkerThread
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.clevertap.android.geofence.CTGeofenceConstants.TAG_WORK_LOCATION_UPDATES
import com.clevertap.android.geofence.interfaces.CTLocationAdapter
import com.clevertap.android.geofence.interfaces.CTLocationCallback
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit

/**
 * Communicates with [FusedLocationProviderClient] and [WorkManager] to
 * Request/Remove Location updates based on Location fetch modes -
 * ([CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC]) or
 * ([CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC]) and config settings
 */
internal class GoogleLocationAdapter(context: Context) : CTLocationAdapter {

    private var backgroundLocationUpdatesEnabled = false

    private val context: Context = context.applicationContext

    private var fastestInterval: Long = 0

    private val fusedProviderClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(this.context)

    private var interval: Long = 0

    /**
     * Applicable only for ([CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC])
     */
    private var locationAccuracy = Priority.PRIORITY_HIGH_ACCURACY

    private var locationFetchMode = 0

    private var smallestDisplacement = 0f

    /**
     * Gets last known location from [FusedLocationProviderClient] and delivers it to caller
     * through [CTLocationCallback]
     *
     * **Must be called from background thread**
     *
     * @param callback instance of [CTLocationCallback]
     */
    @WorkerThread
    override fun getLastLocation(callback: CTLocationCallback) {
        //thread safe

        CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, "Requesting Last Location..")

        var location: Location? = null
        try {
            @SuppressLint("MissingPermission")
            val lastLocation: Task<Location> = fusedProviderClient.lastLocation

            // blocking task
            location = Tasks.await(lastLocation)

            if (location != null) {
                CTGeofenceAPI.getLogger().debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "New Location = " + location.latitude + "," +
                            location.longitude
                )
            }

            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, "Last location request completed")
        } catch (e: Exception) {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Failed to request last location", e
            )
        } finally {
            callback.onLocationComplete(location)
        }
    }

    /**
     * Helper method to remove location updates for both fetch modes ([CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC])
     * and
     * ([CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC])
     *
     * **Must be called from background thread**
     *
     * @param pendingIntent instance of [PendingIntent] of type
     * [PendingIntentFactory.PENDING_INTENT_LOCATION]
     */
    @WorkerThread
    override fun removeLocationUpdates(pendingIntent: PendingIntent?) {
        clearLocationUpdates(pendingIntent)
        clearLocationWorkRequest()
    }

    /**
     * Communicates with [FusedLocationProviderClient] and [WorkManager] to
     * Request/Remove Location updates based on Location fetch modes -
     * ([CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC]) or
     * ([CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC]) and config settings
     *
     * **Must be called from background thread**
     */
    @WorkerThread
    override fun requestLocationUpdates() {
        CTGeofenceAPI.getLogger().debug(
            CTGeofenceAPI.GEOFENCE_LOG_TAG,
            "requestLocationUpdates() called"
        )

        applySettings(context)

        if (!backgroundLocationUpdatesEnabled) {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "not requesting location updates since background location updates is not enabled"
            )
            if (CTGeofenceAPI.getInstance(context).cleverTapApi != null) {
                CTGeofenceAPI.getInstance(context)
                    .cleverTapApi!!
                    .pushGeoFenceError(
                        CTGeofenceConstants.ERROR_CODE,
                        "not requesting location updates since background location updates is not enabled"
                    )
            }
            return
        }

        if (locationFetchMode == CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC.toInt()) {
            // should get same pendingIntent on each app launch or else instance will leak
            val pendingIntent = PendingIntentFactory.getPendingIntent(
                context,
                PendingIntentFactory.PENDING_INTENT_LOCATION, FLAG_UPDATE_CURRENT
            )

            clearLocationWorkRequest()

            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "requesting current location periodically.."
            )

            try {
                // will overwrite location request if change in location config is detected
                @SuppressLint("MissingPermission")
                val requestLocationUpdatesTask: Task<Void> = fusedProviderClient
                    .requestLocationUpdates(getLocationRequest(), pendingIntent!!)

                // blocking task
                Tasks.await(requestLocationUpdatesTask)

                CTGeofenceAPI.getLogger().debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Finished requesting current location periodically.."
                )
            } catch (e: Exception) {
                CTGeofenceAPI.getLogger().debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Failed to request location updates", e
                )
            }
        } else {
            // remove previously registered location request
            val pendingIntent = PendingIntentFactory.getPendingIntent(
                context,
                PendingIntentFactory.PENDING_INTENT_LOCATION, FLAG_NO_CREATE
            )

            clearLocationUpdates(pendingIntent)

            // start periodic work for location updates
            scheduleManualLocationUpdates()
        }
    }

    /**
     * Retrieves config settings from [CTGeofenceSettings] provided by client through
     * [CTGeofenceAPI.init]
     *
     * @param context application [Context]
     */
    private fun applySettings(context: Context) {
        var geofenceSettings = CTGeofenceAPI.getInstance(context).geofenceSettings

        if (geofenceSettings == null) {
            geofenceSettings = CTGeofenceAPI.getInstance(context).initDefaultConfig()
        }

        locationFetchMode = geofenceSettings.locationFetchMode
        backgroundLocationUpdatesEnabled = geofenceSettings.isBackgroundLocationUpdatesEnabled
        interval = geofenceSettings.interval
        fastestInterval = geofenceSettings.fastestInterval
        smallestDisplacement = geofenceSettings.smallestDisplacement

        val accuracy = geofenceSettings.locationAccuracy
        when (accuracy) {
            1 -> locationAccuracy = Priority.PRIORITY_HIGH_ACCURACY
            2 -> locationAccuracy = Priority.PRIORITY_BALANCED_POWER_ACCURACY
            3 -> locationAccuracy = Priority.PRIORITY_LOW_POWER
        }
    }

    /**
     * Removes Location Update of type ([CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC])
     * using [FusedLocationProviderClient]
     *
     * **Must be called from background thread**
     */
    @WorkerThread
    private fun clearLocationUpdates(pendingIntent: PendingIntent?) {
        if (pendingIntent == null) {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Can't stop location updates since provided pendingIntent is null"
            )
            return
        }

        try {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "removing periodic current location request.."
            )

            val removeLocationUpdatesTask: Task<Void> = fusedProviderClient.removeLocationUpdates(pendingIntent)

            // blocking task
            Tasks.await(removeLocationUpdatesTask)

            pendingIntent.cancel()

            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Successfully removed periodic current location request"
            )
        } catch (e: Exception) {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Failed to remove location updates", e
            )
        }
    }

    /**
     * Removes Periodic Location Update work request
     */
    private fun clearLocationWorkRequest() {
        if (!Utils.isConcurrentFuturesDependencyAvailable()) {
            CTGeofenceAPI.getLogger().info(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "concurrent-futures dependency is missing"
            )
            return
        }

        try {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "removing periodic last location request.."
            )

            WorkManager.getInstance(context).cancelUniqueWork(TAG_WORK_LOCATION_UPDATES)

            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Successfully removed periodic last location request"
            )
        } catch (t: NoClassDefFoundError) {
            CTGeofenceAPI.getLogger().info(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "WorkManager dependency is missing"
            )
        } catch (t: Throwable) {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Failed to cancel location work request", t
            )
        }
    }

    /**
     * Builds an instance of [LocationRequest] using config settings
     *
     * @return an instance of [LocationRequest]
     */
    private fun getLocationRequest(): LocationRequest {
        return LocationRequest.Builder(locationAccuracy, interval)
            .setMinUpdateIntervalMillis(fastestInterval)
            .setMinUpdateDistanceMeters(smallestDisplacement)
            .build()
    }

    /**
     * Schedules Periodic Location Update work request using config settings,
     * if work manager dependencies available
     */
    private fun scheduleManualLocationUpdates() {
        if (!Utils.isConcurrentFuturesDependencyAvailable()) {
            CTGeofenceAPI.getLogger().info(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "concurrent-futures dependency is missing"
            )
            if (CTGeofenceAPI.getInstance(context).cleverTapApi != null) {
                CTGeofenceAPI.getInstance(context)
                    .cleverTapApi!!
                    .pushGeoFenceError(
                        CTGeofenceConstants.ERROR_CODE,
                        "concurrent-futures dependency is missing"
                    )
            }
            return
        }

        CTGeofenceAPI.getLogger().debug(
            CTGeofenceAPI.GEOFENCE_LOG_TAG,
            "Scheduling periodic last location request.."
        )

        try {
            val locationRequest = PeriodicWorkRequest.Builder(
                BackgroundLocationWork::class.java,
                interval, TimeUnit.MILLISECONDS,
                FLEX_INTERVAL_IN_MILLIS, TimeUnit.MILLISECONDS
            )
                .build()

            // schedule unique work request to avoid duplicates
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                TAG_WORK_LOCATION_UPDATES,
                ExistingPeriodicWorkPolicy.KEEP, locationRequest
            )

            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Finished scheduling periodic last location request.."
            )
        } catch (t: NoClassDefFoundError) {
            CTGeofenceAPI.getLogger().info(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "WorkManager dependency is missing"
            )
        } catch (t: Throwable) {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Failed to request periodic work request", t
            )
        }
    }

    companion object {
        /**
         * Applicable for both fetch modes ([CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC]) and
         * ([CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC])
         */
        const val INTERVAL_IN_MILLIS: Long = 30 * 60 * 1000L

        /**
         * Applicable only for ([CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC])
         */
        const val INTERVAL_FASTEST_IN_MILLIS: Long = 30 * 60 * 1000L

        /**
         * Applicable only for ([CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC])
         */
        const val SMALLEST_DISPLACEMENT_IN_METERS: Float = 200f

        /**
         * Applicable only for ([CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC])
         */
        private const val FLEX_INTERVAL_IN_MILLIS = 10 * 60 * 1000L
    }
}
