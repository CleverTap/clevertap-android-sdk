package com.clevertap.android.geofence

import android.content.Context
import androidx.annotation.WorkerThread
import com.clevertap.android.geofence.interfaces.CTGeofenceTask
import com.clevertap.android.geofence.interfaces.CTGeofenceTask.OnCompleteListener
import com.google.android.gms.location.LocationResult
import java.util.concurrent.Future
import androidx.annotation.RestrictTo

/**
 * A task of type [CTGeofenceTask] responsible for sending Location received from OS to
 * CleverTap SDK which will in turn send it to server to fetch latest geofence list.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PushLocationEventTask(
    context: Context,
    private val locationResult: LocationResult
) : CTGeofenceTask {

    private val context: Context = context.applicationContext

    private var onCompleteListener: OnCompleteListener? = null

    /**
     * Creates [com.clevertap.android.sdk.CleverTapAPI] instance if it's null, mostly in killed state and
     * then sends Location to CleverTap SDK which will in turn send it to server to fetch latest geofence list.
     * Also Location will be delivered to APP on main thread through [CTLocationUpdatesListener] and caller
     * will be notified of completion of the task through [OnCompleteListener]
     */
    @WorkerThread
    override fun execute() {
        CTGeofenceAPI.getLogger().debug(
            CTGeofenceAPI.GEOFENCE_LOG_TAG,
            "Executing PushLocationEventTask..."
        )

        if (!Utils.initCTGeofenceApiIfRequired(context)) {
            // if init fails then return without doing any work
            sendOnCompleteEvent()
            return
        }

        try {
            Utils.notifyLocationUpdates(context, locationResult.lastLocation)

            //getCleverTapApi() won't be null here
            var future: Future<*>? = null

            if (locationResult.lastLocation != null) {
                future = CTGeofenceAPI.getInstance(context)
                    .processTriggeredLocation(locationResult.lastLocation!!)
            }

            if (future == null) {
                CTGeofenceAPI.getLogger().verbose(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Dropping location ping event to CT server"
                )
                return
            }

            CTGeofenceAPI.getLogger().verbose(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Calling future for setLocationForGeofences()"
            )

            future.get()

            CTGeofenceAPI.getLogger().verbose(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Finished calling future for setLocationForGeofences()"
            )
        } catch (e: Exception) {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Failed to push location event to CT", e
            )
        } finally {
            sendOnCompleteEvent()
        }
    }

    override fun setOnCompleteListener(onCompleteListener: OnCompleteListener) {
        this.onCompleteListener = onCompleteListener
    }

    /**
     * Notifies listeners when task execution completes
     */
    private fun sendOnCompleteEvent() {
        if (onCompleteListener != null) {
            onCompleteListener!!.onComplete()
        }
    }
}
