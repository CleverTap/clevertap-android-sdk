package com.clevertap.android.geofence;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import com.clevertap.android.geofence.interfaces.CTGeofenceAdapter;
import com.clevertap.android.geofence.model.CTGeofence;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.util.ArrayList;
import java.util.List;

/**
 * Communicates with {@link GeofencingClient} to
 * Register and Unregister geofences to and from OS respectively
 */
class GoogleGeofenceAdapter implements CTGeofenceAdapter {

    private static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = Geofence.NEVER_EXPIRE;

    static final int GEOFENCE_NOTIFICATION_RESPONSIVENESS_IN_MILLISECONDS = 0;

    private final Context context;

    private final GeofencingClient geofencingClient;

    GoogleGeofenceAdapter(@NonNull Context context) {
        this.context = context.getApplicationContext();
        geofencingClient = LocationServices.getGeofencingClient(this.context);
    }

    /**
     * Registers list of geofences to OS for monitoring, using {@link GeofencingClient}
     * <br><br>
     * <b>Must be called from background thread</b>
     *
     * @param fenceList         list of {@link CTGeofence}
     * @param onSuccessListener callback for successful registration to OS
     */
    @SuppressWarnings("unchecked")
    @WorkerThread
    @Override
    public void addAllGeofence(@Nullable List<CTGeofence> fenceList,
            @NonNull final OnSuccessListener onSuccessListener) {

        if (fenceList == null || fenceList.isEmpty()) {
            return;
        }

        ArrayList<Geofence> googleFenceList = getGoogleGeofences(fenceList);
        Void aVoid = null;

        try {
            // should get same pendingIntent on each app launch or else instance will leak
            PendingIntent geofencePendingIntent = PendingIntentFactory.getPendingIntent(context,
                    PendingIntentFactory.PENDING_INTENT_GEOFENCE, FLAG_UPDATE_CURRENT);

            @SuppressLint("MissingPermission")
            Task<Void> addGeofenceTask = geofencingClient
                    .addGeofences(getGeofencingRequest(googleFenceList), geofencePendingIntent);
            // blocking task
            aVoid = Tasks.await(addGeofenceTask);
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, "Geofence registered successfully");

        } catch (Exception e) {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Failed to add geofences for monitoring");
            e.printStackTrace();
        } finally {
            onSuccessListener.onSuccess(aVoid);
        }

    }

    /**
     * Unregisters list of geofences from OS to stop monitoring, using {@link GeofencingClient}
     * <br><br>
     * <b>Must be called from background thread</b>
     *
     * @param fenceIdList       list of {@link CTGeofence} Ids to unregister
     * @param onSuccessListener callback for successful removal of Geofences from OS
     */
    @SuppressWarnings("unchecked")
    @WorkerThread
    @Override
    public void removeAllGeofence(@Nullable List<String> fenceIdList,
            @NonNull final OnSuccessListener onSuccessListener) {

        if (fenceIdList == null || fenceIdList.isEmpty()) {
            return;
        }

        Void aVoid = null;
        try {
            Task<Void> removeGeofenceTask = geofencingClient.removeGeofences(fenceIdList);
            // blocking task
            aVoid = Tasks.await(removeGeofenceTask);
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, "Geofence removed successfully");

        } catch (Exception e) {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Failed to remove registered geofences");
            e.printStackTrace();
        } finally {
            onSuccessListener.onSuccess(aVoid);
        }
    }

    /**
     * Same as {@link #removeAllGeofence(List, OnSuccessListener)} but uses {@link PendingIntent}
     * of type {@link PendingIntentFactory#PENDING_INTENT_GEOFENCE}
     * <br><br>
     * <b>Must be called from background thread</b>
     *
     * @param pendingIntent of type {@link PendingIntentFactory#PENDING_INTENT_GEOFENCE}
     */
    @WorkerThread
    @Override
    public void stopGeofenceMonitoring(@Nullable final PendingIntent pendingIntent) {

        if (pendingIntent == null) {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Can't stop geofence monitoring since provided pendingIntent is null");
            return;
        }

        try {
            Task<Void> removeGeofenceTask = geofencingClient.removeGeofences(pendingIntent);

            // blocking task
            Tasks.await(removeGeofenceTask);

            // cancel pending intent when no further updates required
            pendingIntent.cancel();
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, "Geofence removed successfully");

        } catch (Exception e) {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Failed to remove registered geofences");
            e.printStackTrace();
        }
    }

    /**
     * Builds an instance of {@link GeofencingRequest} using list of {@link Geofence} and
     * {@link GeofencingRequest#INITIAL_TRIGGER_ENTER}
     *
     * @param googleFenceList list of {@link Geofence}
     * @return an instance of {@link GeofencingRequest}
     */
    private GeofencingRequest getGeofencingRequest(ArrayList<Geofence> googleFenceList) {
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(googleFenceList)
                .build();
    }

    /**
     * Converts list of {@link CTGeofence} to list of {@link Geofence} that can be used to build
     * an instance of {@link GeofencingRequest}
     *
     * @param fenceList list of {@link CTGeofence}
     * @return list of {@link Geofence}
     */
    @NonNull
    private ArrayList<Geofence> getGoogleGeofences(@NonNull List<CTGeofence> fenceList) {
        ArrayList<Geofence> googleFenceList = new ArrayList<>();

        CTGeofenceSettings geofenceSettings = CTGeofenceAPI.getInstance(context).getGeofenceSettings();
        int geofenceNotificationResponsiveness = GEOFENCE_NOTIFICATION_RESPONSIVENESS_IN_MILLISECONDS;
        if (geofenceSettings != null) {
            geofenceNotificationResponsiveness = geofenceSettings.getGeofenceNotificationResponsiveness();
        }

        for (CTGeofence ctGeofence : fenceList) {
            googleFenceList.add(new Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId(ctGeofence.getId())
                    .setNotificationResponsiveness(geofenceNotificationResponsiveness)

                    .setCircularRegion(ctGeofence.getLatitude(), ctGeofence.getLongitude(),
                            ctGeofence.getRadius())
                    .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build());
        }
        return googleFenceList;
    }
}
