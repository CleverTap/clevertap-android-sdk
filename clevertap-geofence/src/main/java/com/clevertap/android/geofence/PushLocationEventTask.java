package com.clevertap.android.geofence;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import com.clevertap.android.geofence.interfaces.CTGeofenceTask;
import com.clevertap.android.geofence.interfaces.CTLocationUpdatesListener;
import com.google.android.gms.location.LocationResult;
import java.util.concurrent.Future;

/**
 * A task of type {@link CTGeofenceTask} responsible for sending Location received from OS to
 * CleverTap SDK which will in turn send it to server to fetch latest geofence list.
 */
class PushLocationEventTask implements CTGeofenceTask {

    private final Context context;

    @NonNull
    private final LocationResult locationResult;

    @Nullable
    private OnCompleteListener onCompleteListener;

    PushLocationEventTask(Context context, @NonNull LocationResult locationResult) {
        this.context = context.getApplicationContext();
        this.locationResult = locationResult;
    }

    /**
     * Creates {@link com.clevertap.android.sdk.CleverTapAPI} instance if it's null, mostly in killed state and
     * then sends Location to CleverTap SDK which will in turn send it to server to fetch latest geofence list.
     * Also Location will be delivered to APP on main thread through {@link CTLocationUpdatesListener} and caller
     * will be notified of completion of the task through {@link OnCompleteListener}
     */
    @WorkerThread
    @Override
    public void execute() {

        CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Executing PushLocationEventTask...");

        if (!Utils.initCTGeofenceApiIfRequired(context)) {
            // if init fails then return without doing any work
            sendOnCompleteEvent();
            return;
        }

        try {
            Utils.notifyLocationUpdates(context, locationResult.getLastLocation());

            @SuppressWarnings("ConstantConditions") //getCleverTapApi() won't be null here
                    Future<?> future = null;

            if (locationResult.getLastLocation() != null) {
                future = CTGeofenceAPI.getInstance(context)
                        .processTriggeredLocation(locationResult.getLastLocation());
            }

            if (future == null) {
                CTGeofenceAPI.getLogger().verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "Dropping location ping event to CT server");
                return;
            }

            CTGeofenceAPI.getLogger().verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Calling future for setLocationForGeofences()");

            future.get();

            CTGeofenceAPI.getLogger().verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Finished calling future for setLocationForGeofences()");
        } catch (Exception e) {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Failed to push location event to CT");
            e.printStackTrace();
        } finally {
            sendOnCompleteEvent();
        }


    }

    @Override
    public void setOnCompleteListener(@NonNull OnCompleteListener onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
    }

    /**
     * Notifies listeners when task execution completes
     */
    private void sendOnCompleteEvent() {
        if (onCompleteListener != null) {
            onCompleteListener.onComplete();
        }
    }
}
