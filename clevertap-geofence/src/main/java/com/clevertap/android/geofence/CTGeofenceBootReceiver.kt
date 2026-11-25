package com.clevertap.android.geofence

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.MainThread
import com.clevertap.android.geofence.CTGeofenceAPI.Companion.GEOFENCE_LOG_TAG
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * A [BroadcastReceiver] which will reset geofence monitoring and location updates on device boot
 */
internal class CTGeofenceBootReceiver : BroadcastReceiver() {

    /**
     * Validates necessary location permissions and creates [GeofenceUpdateTask] and
     * [LocationUpdateTask] to sends it to Queue
     * using [CTGeofenceTaskManager]
     *
     * @param context application [Context]
     * @param intent  an instance of [Intent]
     */
    @MainThread
    override fun onReceive(context: Context, intent: Intent?) {
        val applicationContext = context.applicationContext

        CTGeofenceAPI.getLogger().debug(
            GEOFENCE_LOG_TAG, "onReceive called after " +
                    "device reboot"
        )

        if (intent == null || intent.action == null) {
            return
        }

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (!Utils.hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
                CTGeofenceAPI.getLogger().debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "We don't have ACCESS_FINE_LOCATION permission! Not registering " +
                            "geofences and location updates after device reboot"
                )
                return
            }

            if (!Utils.hasBackgroundLocationPermission(context)) {
                CTGeofenceAPI.getLogger().debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "We don't have ACCESS_BACKGROUND_LOCATION permission! not registering " +
                            "geofences and location updates after device reboot"
                )
                return
            }

            val result = goAsync()

            val thread: Thread = object : Thread() {
                override fun run() {
                    try {
                        if (!Utils.initCTGeofenceApiIfRequired(applicationContext)) {
                            // if init fails then return without doing any work
                            finishPendingIntent(result)
                            return
                        }

                        CTGeofenceAPI.getLogger().info(
                            GEOFENCE_LOG_TAG,
                            "registering geofences after device reboot"
                        )

                        // pass null GeofenceList to register old fences stored in file
                        val geofenceUpdateTask = GeofenceUpdateTask(applicationContext, null)

                        val futureGeofence = CTGeofenceTaskManager.getInstance()
                            .postAsyncSafely("ProcessGeofenceUpdatesOnBoot", geofenceUpdateTask)

                        try {
                            futureGeofence?.get(BROADCAST_INTENT_TIME_MS, TimeUnit.MILLISECONDS)
                        } catch (e: TimeoutException) {
                            CTGeofenceAPI.getLogger().debug(
                                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                "Timeout geofence update task execution limit of 3 secs", e
                            )
                        } catch (e: Exception) {
                            CTGeofenceAPI.getLogger().debug(
                                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                "Exception while executing geofence update task", e
                            )
                        }

                        CTGeofenceAPI.getLogger().info(
                            GEOFENCE_LOG_TAG,
                            "registering location updates after device reboot"
                        )

                        val locationUpdateTask = LocationUpdateTask(applicationContext)

                        val futureLocation = CTGeofenceTaskManager.getInstance()
                            .postAsyncSafely("IntitializeLocationUpdatesOnBoot", locationUpdateTask)

                        try {
                            futureLocation?.get(BROADCAST_INTENT_TIME_MS, TimeUnit.MILLISECONDS)
                        } catch (e: TimeoutException) {
                            CTGeofenceAPI.getLogger().debug(
                                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                "Timeout location update task execution limit of 3 secs", e
                            )
                        } catch (e: Exception) {
                            CTGeofenceAPI.getLogger().debug(
                                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                "Exception while executing location update task", e
                            )
                        }

                        finishPendingIntent(result)
                    } catch (e: Exception) {
                        finishPendingIntent(result)

                        CTGeofenceAPI.getLogger().debug(
                            CTGeofenceAPI.GEOFENCE_LOG_TAG,
                            "Exception while processing Boot receiver intent", e
                        )
                    }
                }
            }
            thread.start()
        }
    }

    private fun finishPendingIntent(result: PendingResult?) {
        if (result != null) {
            result.finish()

            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Boot receiver Pending Intent is finished"
            )
        }
    }

    companion object {
        /**
         * Timeout to prevent ANR
         */
        private const val BROADCAST_INTENT_TIME_MS: Long = 3000
    }
}
