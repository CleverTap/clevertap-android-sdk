package com.clevertap.android.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.MainThread
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import androidx.annotation.RestrictTo

/**
 * A [BroadcastReceiver] which receives geofence enter/exit event updates in foreground as well as in background
 * and in killed state.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal open class CTGeofenceReceiver : BroadcastReceiver() {

    /**
     * Creates [PushGeofenceEventTask] and sends it to Queue using [CTGeofenceTaskManager]
     *
     * @param context application [Context]
     * @param intent  an instance of [Intent] containing triggered
     * [com.google.android.gms.location.GeofencingEvent]
     */
    @MainThread
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) {
            return
        }

        val result = goAsync()

        try {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Geofence receiver called"
            )

            val thread: Thread = object : Thread() {
                override fun run() {
                    val pushGeofenceEventTask = PushGeofenceEventTask(context, intent)

                    val future = CTGeofenceTaskManager.getInstance().postAsyncSafely(
                        "PushGeofenceEvent",
                        pushGeofenceEventTask
                    )

                    try {
                        future?.get(BROADCAST_INTENT_TIME_MS, TimeUnit.MILLISECONDS)
                    } catch (e: TimeoutException) {
                        CTGeofenceAPI.getLogger().debug(
                            CTGeofenceAPI.GEOFENCE_LOG_TAG,
                            "Timeout geofence receiver execution limit of 8 secs", e
                        )
                    } catch (e: Exception) {
                        CTGeofenceAPI.getLogger().debug(
                            CTGeofenceAPI.GEOFENCE_LOG_TAG,
                            "Exception while processing geofence receiver intent", e
                        )
                    }

                    if (result != null) {
                        result.finish()

                        CTGeofenceAPI.getLogger().debug(
                            CTGeofenceAPI.GEOFENCE_LOG_TAG,
                            "Geofence receiver Pending Intent is finished"
                        )
                    }
                }
            }
            thread.start()
        } catch (e: Exception) {
            if (result != null) {
                result.finish()

                CTGeofenceAPI.getLogger().debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Geofence receiver Pending Intent is finished"
                )
            }

            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Exception while processing geofence receiver intent", e
            )
        }

        CTGeofenceAPI.getLogger().debug(
            CTGeofenceAPI.GEOFENCE_LOG_TAG,
            "Returning from Geofence receiver"
        )
    }

    companion object {
        /**
         * Timeout to prevent ANR
         */
        private const val BROADCAST_INTENT_TIME_MS: Long = 8000
    }
}
