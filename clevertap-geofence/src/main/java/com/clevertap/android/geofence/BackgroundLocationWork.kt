package com.clevertap.android.geofence

import android.content.Context
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.clevertap.android.geofence.interfaces.CTLocationCallback
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Future

/**
 * A [ListenableWorker] which fetches last known location from OS in foreground as well as in background
 * and in killed state. This will be active when location fetch mode set by client is
 * [CTGeofenceSettings.FETCH_LAST_LOCATION_PERIODIC]
 * Frequency of location updates depends on [CTGeofenceSettings.Builder.setInterval]
 */
internal class BackgroundLocationWork(context: Context, workerParams: WorkerParameters) :
    ListenableWorker(context, workerParams) {

    /**
     * Fetches last known location from OS and delivers it to APP through [CTLocationUpdatesListener]
     *
     * Fetched Location will be passed to [CTGeofenceAPI.processTriggeredLocation] to send it to
     * server for latest geofence list
     */
    override fun startWork(): ListenableFuture<Result> {
        CTGeofenceAPI.getLogger().debug(
            CTGeofenceAPI.GEOFENCE_LOG_TAG,
            "Handling work in BackgroundLocationWork..."
        )

        val listenableFuture: ListenableFuture<Result> = CallbackToFutureAdapter
            .getFuture { completer ->
                val ctLocationCallback = CTLocationCallback { location ->
                    // running on bg thread

                    val cleverTapApi = CTGeofenceAPI.getInstance(applicationContext)
                        .cleverTapApi
                    var future: Future<*>? = null

                    if (cleverTapApi != null && location != null) {
                        future = CTGeofenceAPI.getInstance(applicationContext)
                            .processTriggeredLocation(location)
                    }

                    try {
                        Utils.notifyLocationUpdates(applicationContext, location)

                        future?.get()
                    } catch (e: Exception) {
                        CTGeofenceAPI.getLogger().debug(
                            CTGeofenceAPI.GEOFENCE_LOG_TAG,
                            "Exception while processing geofence receiver intent", e
                        )
                        cleverTapApi?.pushGeoFenceError(
                            CTGeofenceConstants.ERROR_CODE,
                            "Exception while processing geofence receiver intent"
                        )
                    }

                    completer.set(Result.success())

                    CTGeofenceAPI.getLogger().debug(
                        CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "BackgroundLocationWork is finished"
                    )
                }

                CTGeofenceTaskManager.getInstance().postAsyncSafely(
                    "ProcessLocationWork",
                    Runnable {
                        if (!Utils.initCTGeofenceApiIfRequired(applicationContext)) {
                            // if init fails then return without doing any work
                            completer.set(Result.success())
                            return@Runnable
                        }

                        val ctLocationAdapter = CTGeofenceAPI
                            .getInstance(applicationContext).ctLocationAdapter

                        if (ctLocationAdapter != null) {
                            ctLocationAdapter.getLastLocation(ctLocationCallback)
                        } else {
                            completer.set(Result.success())
                        }
                    })

                ctLocationCallback
            }

        return listenableFuture
    }
}
