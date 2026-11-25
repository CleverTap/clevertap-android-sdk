package com.clevertap.android.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.MainThread
import com.google.android.gms.location.LocationResult
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * A [BroadcastReceiver] which receives location updates in foreground as well as in background
 * and in killed state. This will be active when location fetch mode set by client is
 * [CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC]
 * Accuracy and Frequency of location updates depends on accuracy, interval, fastest interval and
 * smallest displacement as set by Client through [CTGeofenceAPI.init]
 */
internal class CTLocationUpdateReceiver : BroadcastReceiver() {

    /**
     * Creates [PushLocationEventTask] and sends it to Queue using [CTGeofenceTaskManager]
     *
     * @param context application [Context]
     * @param intent  an instance of [Intent] containing current location of user
     */
    @MainThread
    override fun onReceive(context: Context, intent: Intent?) {
        val result = goAsync()

        try {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Location updates receiver called"
            )

            val locationResult = LocationResult.extractResult(intent!!)

            if (locationResult != null && locationResult.lastLocation != null) {
                val thread: Thread = object : Thread() {
                    override fun run() {
                        val pushLocationEventTask = PushLocationEventTask(context, locationResult)

                        val future = CTGeofenceTaskManager.getInstance()
                            .postAsyncSafely("PushLocationEvent", pushLocationEventTask)

                        try {
                            future?.get(BROADCAST_INTENT_TIME_MS, TimeUnit.MILLISECONDS)
                        } catch (e: TimeoutException) {
                            CTGeofenceAPI.getLogger().debug(
                                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                "Timeout location receiver execution limit of 10 secs"
                            )
                        } catch (e: Exception) {
                            CTGeofenceAPI.getLogger().debug(
                                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                "Exception while processing location receiver intent", e
                            )
                        }

                        finishPendingIntent(result)
                    }
                }

                thread.start()
            } else {
                CTGeofenceAPI.getLogger().debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Location Result is null"
                )

                finishPendingIntent(result)
            }
        } catch (e: Exception) {
            finishPendingIntent(result)

            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Exception while processing location updates receiver intent", e
            )
        }

        CTGeofenceAPI.getLogger().debug(
            CTGeofenceAPI.GEOFENCE_LOG_TAG,
            "Returning from Location Updates Receiver"
        )
    }

    private fun finishPendingIntent(result: PendingResult?) {
        if (result != null) {
            result.finish()

            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Location receiver Pending Intent is finished"
            )
        }
    }

    companion object {
        /**
         * Timeout to prevent ANR
         */
        private const val BROADCAST_INTENT_TIME_MS: Long = 8000
    }
}
