package com.clevertap.android.geofence.interfaces;

import android.app.PendingIntent;
import com.clevertap.android.geofence.model.CTGeofence;
import com.google.android.gms.tasks.OnSuccessListener;
import java.util.List;

/**
 * Interface that defines API-type(Google APIs/Android platform APIs) class that can be used to add and remove
 * geofences
 * to and from OS respectively
 */
public interface CTGeofenceAdapter {

    /**
     * Registers list of geofences to OS for monitoring
     *
     * @param fenceList         list of {@link CTGeofence}
     * @param onSuccessListener callback on successful registration
     */
    void addAllGeofence(List<CTGeofence> fenceList, OnSuccessListener onSuccessListener);

    /**
     * Unregisters list of geofences from OS to stop monitoring
     *
     * @param fenceIdList       list of {@link CTGeofence} Ids
     * @param onSuccessListener callback on successful un-registration
     */
    void removeAllGeofence(List<String> fenceIdList, OnSuccessListener onSuccessListener);


    /**
     * Unregisters all geofences from OS associated with the given {@link PendingIntent} to stop monitoring
     *
     * @param pendingIntent instance of {@link PendingIntent}
     */
    void stopGeofenceMonitoring(PendingIntent pendingIntent);

}
