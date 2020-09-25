package com.clevertap.android.geofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import com.clevertap.android.sdk.CleverTapAPI;
import com.google.android.gms.location.LocationResult;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link BroadcastReceiver} which receives location updates in foreground as well as in background
 * and in killed state. This will be active when location fetch mode set by client is
 * {@link CTGeofenceSettings#FETCH_CURRENT_LOCATION_PERIODIC}<br>
 * Accuracy and Frequency of location updates depends on accuracy, interval, fastest interval and
 * smallest displacement as set by Client through {@link CTGeofenceAPI#init(CTGeofenceSettings, CleverTapAPI)}
 */
public class CTLocationUpdateReceiver extends BroadcastReceiver {

    /**
     * Timeout to prevent ANR
     */
    private static final long BROADCAST_INTENT_TIME_MS = 8000;

    /**
     * Creates {@link PushLocationEventTask} and sends it to Queue using {@link CTGeofenceTaskManager}
     *
     * @param context application {@link Context}
     * @param intent  an instance of {@link Intent} containing current location of user
     */
    @MainThread
    @Override
    public void onReceive(final Context context, final Intent intent) {

        final PendingResult result = goAsync();

        try {

            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Location updates receiver called");

            final LocationResult locationResult = LocationResult.extractResult(intent);

            if (locationResult != null && locationResult.getLastLocation() != null) {
                Thread thread = new Thread() {
                    public void run() {

                        PushLocationEventTask pushLocationEventTask =
                                new PushLocationEventTask(context, locationResult);

                        Future<?> future = CTGeofenceTaskManager.getInstance()
                                .postAsyncSafely("PushLocationEvent", pushLocationEventTask);

                        try {
                            if (future != null) {
                                future.get(BROADCAST_INTENT_TIME_MS, TimeUnit.MILLISECONDS);
                            }
                        } catch (TimeoutException e) {
                            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                    "Timeout location receiver execution limit of 10 secs");
                        } catch (Exception e) {
                            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                    "Exception while processing location receiver intent");
                            e.printStackTrace();
                        }

                        finishPendingIntent(result);
                    }
                };

                thread.start();
            } else {

                CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "Location Result is null");

                finishPendingIntent(result);
            }

        } catch (Exception e) {

            finishPendingIntent(result);

            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Exception while processing location updates receiver intent");
            e.printStackTrace();
        }

        CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Returning from Location Updates Receiver");

    }

    private void finishPendingIntent(@Nullable PendingResult result) {
        if (result != null) {
            result.finish();

            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Location receiver Pending Intent is finished");
        }
    }

}
