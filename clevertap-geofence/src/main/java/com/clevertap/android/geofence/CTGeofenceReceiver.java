package com.clevertap.android.geofence;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.annotation.MainThread;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A {@link BroadcastReceiver} which receives geofence enter/exit event updates in foreground as well as in background
 * and in killed state.
 */
public class CTGeofenceReceiver extends BroadcastReceiver {

    /**
     * Timeout to prevent ANR
     */
    private static final long BROADCAST_INTENT_TIME_MS = 8000;

    /**
     * Creates {@link PushGeofenceEventTask} and sends it to Queue using {@link CTGeofenceTaskManager}
     *
     * @param context application {@link Context}
     * @param intent  an instance of {@link Intent} containing triggered
     *                {@link com.google.android.gms.location.GeofencingEvent}
     */
    @MainThread
    @Override
    public void onReceive(final Context context, final Intent intent) {

        if (intent == null) {
            return;
        }

        final PendingResult result = goAsync();

        try {

            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Geofence receiver called");

            Thread thread = new Thread() {
                public void run() {
                    PushGeofenceEventTask pushGeofenceEventTask = new PushGeofenceEventTask(context, intent);

                    Future<?> future = CTGeofenceTaskManager.getInstance().postAsyncSafely("PushGeofenceEvent",
                            pushGeofenceEventTask);

                    try {
                        if (future != null) {
                            future.get(BROADCAST_INTENT_TIME_MS, TimeUnit.MILLISECONDS);
                        }
                    } catch (TimeoutException e) {
                        CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                "Timeout geofence receiver execution limit of 10 secs");
                    } catch (Exception e) {
                        CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                "Exception while processing geofence receiver intent");
                        e.printStackTrace();
                    }

                    if (result != null) {
                        result.finish();

                        CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                "Geofence receiver Pending Intent is finished");
                    }
                }
            };
            thread.start();

        } catch (Exception e) {

            if (result != null) {
                result.finish();

                CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "Geofence receiver Pending Intent is finished");
            }

            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Exception while processing geofence receiver intent");
            e.printStackTrace();
        }

        CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Returning from Geofence receiver");
    }
}
