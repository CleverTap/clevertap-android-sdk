package com.clevertap.android.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import androidx.annotation.WorkerThread
import com.clevertap.android.geofence.interfaces.CTGeofenceAdapter
import com.clevertap.android.geofence.model.CTGeofence
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import androidx.annotation.RestrictTo

/**
 * Communicates with [GeofencingClient] to
 * Register and Unregister geofences to and from OS respectively
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class GoogleGeofenceAdapter(context: Context) : CTGeofenceAdapter {

    private val context: Context = context.applicationContext

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(this.context)

    /**
     * Registers list of geofences to OS for monitoring, using [GeofencingClient]
     *
     * **Must be called from background thread**
     *
     * @param fenceList         list of [CTGeofence]
     * @param onSuccessListener callback for successful registration to OS
     */
    @WorkerThread
    override fun addAllGeofence(
        fenceList: List<CTGeofence>?,
        onSuccessListener: OnSuccessListener<Void>
    ) {
        if (fenceList.isNullOrEmpty()) {
            return
        }

        val googleFenceList = getGoogleGeofences(fenceList)

        try {
            // should get same pendingIntent on each app launch or else instance will leak
            val geofencePendingIntent = PendingIntentFactory.getPendingIntent(
                context,
                PendingIntentFactory.PENDING_INTENT_GEOFENCE, FLAG_UPDATE_CURRENT
            )

            @SuppressLint("MissingPermission")
            val addGeofenceTask: Task<Void> = geofencingClient
                .addGeofences(getGeofencingRequest(googleFenceList), geofencePendingIntent!!)
            // blocking task
            Tasks.await(addGeofenceTask)
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, "Geofence registered successfully")
        } catch (e: Exception) {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Failed to add geofences for monitoring", e
            )
        } finally {
            // Tasks.await on a Task<Void> only ever yields null
            onSuccessListener.onSuccess(null)
        }
    }

    /**
     * Unregisters list of geofences from OS to stop monitoring, using [GeofencingClient]
     *
     * **Must be called from background thread**
     *
     * @param fenceIdList       list of [CTGeofence] Ids to unregister
     * @param onSuccessListener callback for successful removal of Geofences from OS
     */
    @WorkerThread
    override fun removeAllGeofence(
        fenceIdList: List<String>?,
        onSuccessListener: OnSuccessListener<Void>
    ) {
        if (fenceIdList.isNullOrEmpty()) {
            return
        }

        try {
            val removeGeofenceTask: Task<Void> = geofencingClient.removeGeofences(fenceIdList)
            // blocking task
            Tasks.await(removeGeofenceTask)
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, "Geofence removed successfully")
        } catch (e: Exception) {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Failed to remove registered geofences", e
            )
        } finally {
            // Tasks.await on a Task<Void> only ever yields null
            onSuccessListener.onSuccess(null)
        }
    }

    /**
     * Same as [removeAllGeofence] but uses [PendingIntent]
     * of type [PendingIntentFactory.PENDING_INTENT_GEOFENCE]
     *
     * **Must be called from background thread**
     *
     * @param pendingIntent of type [PendingIntentFactory.PENDING_INTENT_GEOFENCE]
     */
    @WorkerThread
    override fun stopGeofenceMonitoring(pendingIntent: PendingIntent?) {
        if (pendingIntent == null) {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Can't stop geofence monitoring since provided pendingIntent is null"
            )
            return
        }

        try {
            val removeGeofenceTask: Task<Void> = geofencingClient.removeGeofences(pendingIntent)

            // blocking task
            Tasks.await(removeGeofenceTask)

            // cancel pending intent when no further updates required
            pendingIntent.cancel()
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, "Geofence removed successfully")
        } catch (e: Exception) {
            CTGeofenceAPI.getLogger().debug(
                CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Failed to remove registered geofences", e
            )
        }
    }

    /**
     * Builds an instance of [GeofencingRequest] using list of [Geofence] and
     * [GeofencingRequest.INITIAL_TRIGGER_ENTER]
     *
     * @param googleFenceList list of [Geofence]
     * @return an instance of [GeofencingRequest]
     */
    private fun getGeofencingRequest(googleFenceList: ArrayList<Geofence>): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(googleFenceList)
            .build()
    }

    /**
     * Converts list of [CTGeofence] to list of [Geofence] that can be used to build
     * an instance of [GeofencingRequest]
     *
     * @param fenceList list of [CTGeofence]
     * @return list of [Geofence]
     */
    private fun getGoogleGeofences(fenceList: List<CTGeofence>): ArrayList<Geofence> {
        val googleFenceList = ArrayList<Geofence>()

        val geofenceSettings = CTGeofenceAPI.getInstance(context).geofenceSettings
        var geofenceNotificationResponsiveness = GEOFENCE_NOTIFICATION_RESPONSIVENESS_IN_MILLISECONDS
        if (geofenceSettings != null) {
            geofenceNotificationResponsiveness = geofenceSettings.geofenceNotificationResponsiveness
        }

        CTGeofenceAPI.getLogger().debug(
            CTGeofenceAPI.GEOFENCE_LOG_TAG,
            "Setting geofenceNotificationResponsiveness to $geofenceNotificationResponsiveness"
        )

        for (ctGeofence in fenceList) {
            googleFenceList.add(
                Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId(ctGeofence.id)
                    .setNotificationResponsiveness(geofenceNotificationResponsiveness)
                    .setCircularRegion(
                        ctGeofence.latitude, ctGeofence.longitude,
                        ctGeofence.radius.toFloat()
                    )
                    .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                    .setTransitionTypes(
                        Geofence.GEOFENCE_TRANSITION_ENTER or
                                Geofence.GEOFENCE_TRANSITION_EXIT
                    )
                    .build()
            )
        }
        return googleFenceList
    }

    companion object {
        private const val GEOFENCE_EXPIRATION_IN_MILLISECONDS = Geofence.NEVER_EXPIRE

        /**
         * How soon should the callback be called when the transition associated with the Geofence is triggered.
         */
        const val GEOFENCE_NOTIFICATION_RESPONSIVENESS_IN_MILLISECONDS: Int = 0
    }
}
