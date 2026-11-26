package com.clevertap.android.geofence.interfaces

import android.app.PendingIntent
import com.clevertap.android.geofence.model.CTGeofence
import com.google.android.gms.tasks.OnSuccessListener
import androidx.annotation.RestrictTo

/**
 * Interface that defines API-type(Google APIs/Android platform APIs) class that can be used to add and remove
 * geofences
 * to and from OS respectively
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface CTGeofenceAdapter {

    /**
     * Registers list of geofences to OS for monitoring
     *
     * @param fenceList         list of [CTGeofence]
     * @param onSuccessListener callback on successful registration
     */
    fun addAllGeofence(fenceList: List<CTGeofence>?, onSuccessListener: OnSuccessListener<Void>)

    /**
     * Unregisters list of geofences from OS to stop monitoring
     *
     * @param fenceIdList       list of [CTGeofence] Ids
     * @param onSuccessListener callback on successful un-registration
     */
    fun removeAllGeofence(fenceIdList: List<String>?, onSuccessListener: OnSuccessListener<Void>)

    /**
     * Unregisters all geofences from OS associated with the given [PendingIntent] to stop monitoring
     *
     * @param pendingIntent instance of [PendingIntent]
     */
    fun stopGeofenceMonitoring(pendingIntent: PendingIntent?)
}
