package com.clevertap.android.geofence

import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.annotation.WorkerThread
import com.clevertap.android.geofence.interfaces.CTGeofenceEventsListener
import com.clevertap.android.geofence.interfaces.CTGeofenceTask
import com.clevertap.android.geofence.interfaces.CTGeofenceTask.OnCompleteListener
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import org.json.JSONObject
import java.util.concurrent.Future
import androidx.annotation.RestrictTo

/**
 * A task of type [CTGeofenceTask] responsible for processing and sending `GeoCluster Entered`
 * or `GeoCluster Exited` events to CleverTap SDK which will in turn send it to server.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PushGeofenceEventTask(context: Context, private val intent: Intent) : CTGeofenceTask {

    private val context: Context = context.applicationContext

    private var onCompleteListener: OnCompleteListener? = null

    /**
     * Creates [com.clevertap.android.sdk.CleverTapAPI] instance if it's null, mostly in killed state.
     * On Enter or Exit transition triggered [GeofencingEvent] will be sent to [pushGeofenceEvents]
     * for further processing, if it has no error in it.
     * Caller will be notified of completion of the task through [OnCompleteListener]
     */
    @WorkerThread
    override fun execute() {
        CTGeofenceAPI.getLogger().debug(
            CTGeofenceAPI.GEOFENCE_LOG_TAG,
            "Executing PushGeofenceEventTask..."
        )

        if (!Utils.initCTGeofenceApiIfRequired(context.applicationContext)) {
            // if init fails then return without doing any work
            sendOnCompleteEvent()
            return
        }

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        val cleverTapApi = CTGeofenceAPI.getInstance(context).cleverTapApi

        if (geofencingEvent!!.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "error while processing geofence event: $errorMessage"
            )
            cleverTapApi?.pushGeoFenceError(
                CTGeofenceConstants.ERROR_CODE,
                "error while processing geofence event: $errorMessage"
            )
            sendOnCompleteEvent()
            return
        }

        // Get the transition type.
        val geofenceTransition = geofencingEvent.geofenceTransition

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            // Get the geofences that were triggered. A single event can trigger multiple geofences.

            val triggeringGeofences = geofencingEvent.triggeringGeofences
            val triggeringLocation = geofencingEvent.triggeringLocation

            // send geofence event through queue to avoid loss of old geofence data
            // for example. while searching triggered geofence in file, it may be overwritten by new fences received from server

            pushGeofenceEvents(triggeringGeofences, triggeringLocation, geofenceTransition)
        } else {
            // Log the error.
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "invalid geofence transition type: $geofenceTransition"
            )
            cleverTapApi?.pushGeoFenceError(
                CTGeofenceConstants.ERROR_CODE,
                "invalid geofence transition type: $geofenceTransition"
            )
        }

        sendOnCompleteEvent()
    }

    override fun setOnCompleteListener(onCompleteListener: OnCompleteListener) {
        this.onCompleteListener = onCompleteListener
    }

    /**
     * Searches triggered geofences in file and sends them to CleverTap SDK to raise `GeoCluster Entered`
     * or `GeoCluster Exited` events. Error will be sent to CleverTap in case triggered geofence
     * not found in file.
     * Apps will be notified of events through [CTGeofenceEventsListener] on main thread
     *
     * @param triggeringGeofences List of triggered [Geofence]
     * @param triggeringLocation  [Location] object which triggered geofence event
     * @param geofenceTransition  int value of geofence transition event
     */
    @WorkerThread
    private fun pushGeofenceEvents(
        triggeringGeofences: List<Geofence>?,
        triggeringLocation: Location?,
        geofenceTransition: Int
    ) {
        if (triggeringGeofences == null) {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "fetched triggered geofence list is null"
            )
            if (CTGeofenceAPI.getInstance(context).cleverTapApi != null) {
                CTGeofenceAPI.getInstance(context)
                    .cleverTapApi!!
                    .pushGeoFenceError(
                        CTGeofenceConstants.ERROR_CODE,
                        "fetched triggered geofence list is null"
                    )
            }
            return
        }

        // Search triggered geofences in file by id and send stored geofence object to CT SDK
        val oldFenceListString = FileUtils.readFromFile(
            context,
            FileUtils.getCachedFullPath(context, CTGeofenceConstants.CACHED_FILE_NAME)
        )
        if (oldFenceListString.trim { it <= ' ' } != "") {
            val jsonObject: JSONObject
            try {
                jsonObject = JSONObject(oldFenceListString)
                val jsonArray = jsonObject.getJSONArray(CTGeofenceConstants.KEY_GEOFENCES)

                for (triggeredGeofence in triggeringGeofences) {
                    CTGeofenceAPI.getLogger().debug(
                        CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "Searching Triggered geofence with id = " + triggeredGeofence.requestId
                                + " in file..."
                    )

                    var isTriggeredGeofenceFound = false

                    for (i in 0 until jsonArray.length()) {
                        val geofence = jsonArray.getJSONObject(i)
                        if (geofence.getInt(CTGeofenceConstants.KEY_ID).toString() == triggeredGeofence.requestId) {
                            // triggered geofence found in file

                            CTGeofenceAPI.getLogger().debug(
                                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                "Triggered geofence with id = " + triggeredGeofence.requestId
                                        + " is found in file! Sending it to CT SDK"
                            )

                            isTriggeredGeofenceFound = true

                            if (triggeringLocation != null) {
                                geofence.put("triggered_lat", triggeringLocation.latitude)
                                geofence.put("triggered_lng", triggeringLocation.longitude)
                            }

                            // null when the SDK could not queue the event; future!!.get() below then throws
                            // inside the try and is logged as a failed push, exactly as the Java version did.
                            val future: Future<*>?

                            val cleverTapApi = CTGeofenceAPI.getInstance(context).cleverTapApi ?: return

                            val ctGeofenceEventsListener = CTGeofenceAPI
                                .getInstance(context).getCtGeofenceEventsListener()

                            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                                // send event to CleverTap SDK
                                future = cleverTapApi.pushGeofenceEnteredEvent(geofence)

                                // send event to Listener on main thread
                                if (ctGeofenceEventsListener != null) {
                                    com.clevertap.android.sdk.Utils.runOnUiThread {
                                        ctGeofenceEventsListener.onGeofenceEnteredEvent(geofence)
                                    }
                                }
                            } else {
                                // send event to CleverTap SDK
                                future = cleverTapApi.pushGeoFenceExitedEvent(geofence)

                                // send event to Listener on main thread
                                if (ctGeofenceEventsListener != null) {
                                    com.clevertap.android.sdk.Utils.runOnUiThread {
                                        ctGeofenceEventsListener.onGeofenceExitedEvent(geofence)
                                    }
                                }
                            }

                            try {
                                CTGeofenceAPI.getLogger().verbose(
                                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                    "Calling future for geofence event with id = " +
                                            triggeredGeofence.requestId
                                )
                                future!!.get()

                                CTGeofenceAPI.getLogger().verbose(
                                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                    "Finished calling future for geofence event with id = " +
                                            triggeredGeofence.requestId
                                )
                            } catch (e: Exception) {
                                CTGeofenceAPI.getLogger().debug(
                                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                    "Failed to push geofence event with id = " +
                                            triggeredGeofence.requestId, e
                                )
                            }
                            break
                        }
                    }

                    if (!isTriggeredGeofenceFound) {
                        CTGeofenceAPI.getLogger().debug(
                            CTGeofenceAPI.GEOFENCE_LOG_TAG,
                            "Triggered geofence with id = " + triggeredGeofence.requestId
                                    + " is not found in file! Dropping this event"
                        )
                        if (CTGeofenceAPI.getInstance(context).cleverTapApi != null) {
                            CTGeofenceAPI.getInstance(context)
                                .cleverTapApi!!
                                .pushGeoFenceError(
                                    CTGeofenceConstants.ERROR_CODE,
                                    "Triggered geofence with id = " +
                                            triggeredGeofence.requestId
                                            + " is not found in file! Dropping this event"
                                )
                        }
                    }
                }
            } catch (e: Exception) {
                CTGeofenceAPI.getLogger().debug(
                    CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Failed to read triggered geofences from file", e
                )
            }
        }
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
