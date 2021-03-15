package com.clevertap.android.geofence;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import com.clevertap.android.geofence.interfaces.CTGeofenceEventsListener;
import com.clevertap.android.geofence.interfaces.CTGeofenceTask;
import com.clevertap.android.sdk.CleverTapAPI;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingEvent;
import java.util.List;
import java.util.concurrent.Future;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A task of type {@link CTGeofenceTask} responsible for processing and sending {@code GeoCluster Entered}
 * or {@code GeoCluster Exited} events to CleverTap SDK which will in turn send it to server.
 */
class PushGeofenceEventTask implements CTGeofenceTask {

    private final Context context;

    @NonNull
    private final Intent intent;

    @Nullable
    private OnCompleteListener onCompleteListener;

    PushGeofenceEventTask(Context context, @NonNull Intent intent) {
        this.context = context.getApplicationContext();
        this.intent = intent;
    }

    /**
     * Creates {@link com.clevertap.android.sdk.CleverTapAPI} instance if it's null, mostly in killed state.
     * On Enter or Exit transition triggered {@link GeofencingEvent} will be sent to {@link #pushGeofenceEvents(List,
     * Location, int)}
     * for further processing, if it has no error in it.<br>
     * Caller will be notified of completion of the task through {@link OnCompleteListener}
     */
    @WorkerThread
    @Override
    public void execute() {

        CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Executing PushGeofenceEventTask...");

        if (!Utils.initCTGeofenceApiIfRequired(context.getApplicationContext())) {
            // if init fails then return without doing any work
            sendOnCompleteEvent();
            return;
        }

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);

        CleverTapAPI cleverTapApi = CTGeofenceAPI.getInstance(context).getCleverTapApi();

        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.getErrorCode());
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "error while processing geofence event: " + errorMessage);
            if (cleverTapApi != null) {
                cleverTapApi.pushGeoFenceError(CTGeofenceConstants.ERROR_CODE,
                        "error while processing geofence event: " + errorMessage);
            }
            sendOnCompleteEvent();
            return;
        }

        // Get the transition type.
        final int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            // Get the geofences that were triggered. A single event can trigger multiple geofences.

            final List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            final Location triggeringLocation = geofencingEvent.getTriggeringLocation();

            // send geofence event through queue to avoid loss of old geofence data
            // for example. while searching triggered geofence in file, it may be overwritten by new fences received from server

            pushGeofenceEvents(triggeringGeofences, triggeringLocation, geofenceTransition);

        } else {
            // Log the error.
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "invalid geofence transition type: " + geofenceTransition);
            if (cleverTapApi != null) {
                cleverTapApi.pushGeoFenceError(CTGeofenceConstants.ERROR_CODE,
                        "invalid geofence transition type: " + geofenceTransition);
            }
        }

        sendOnCompleteEvent();

    }

    @Override
    public void setOnCompleteListener(@NonNull OnCompleteListener onCompleteListener) {
        this.onCompleteListener = onCompleteListener;
    }

    /**
     * Searches triggered geofences in file and sends them to CleverTap SDK to raise {@code GeoCluster Entered}
     * or {@code GeoCluster Exited} events. Error will be sent to CleverTap in case triggered geofence
     * not found in file.<br>
     * Apps will be notified of events through {@link CTGeofenceEventsListener} on main thread
     *
     * @param triggeringGeofences List of triggered {@link Geofence}
     * @param triggeringLocation  {@link Location} object which triggered geofence event
     * @param geofenceTransition  int value of geofence transition event
     */
    @WorkerThread
    private void pushGeofenceEvents(@Nullable List<Geofence> triggeringGeofences,
            @Nullable Location triggeringLocation,
            int geofenceTransition) {

        if (triggeringGeofences == null) {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "fetched triggered geofence list is null");
            if (CTGeofenceAPI.getInstance(context).getCleverTapApi() != null) {
                CTGeofenceAPI.getInstance(context)
                        .getCleverTapApi()
                        .pushGeoFenceError(CTGeofenceConstants.ERROR_CODE,
                                "fetched triggered geofence list is null");
            }
            return;

        }

        // Search triggered geofences in file by id and send stored geofence object to CT SDK
        String oldFenceListString = FileUtils.readFromFile(context,
                FileUtils.getCachedFullPath(context, CTGeofenceConstants.CACHED_FILE_NAME));
        if (!oldFenceListString.trim().equals("")) {

            JSONObject jsonObject;
            try {
                jsonObject = new JSONObject(oldFenceListString);
                JSONArray jsonArray = jsonObject.getJSONArray(CTGeofenceConstants.KEY_GEOFENCES);

                for (Geofence triggeredGeofence : triggeringGeofences) {

                    CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                            "Searching Triggered geofence with id = " + triggeredGeofence.getRequestId()
                                    + " in file...");

                    boolean isTriggeredGeofenceFound = false;

                    for (int i = 0; i < jsonArray.length(); i++) {
                        final JSONObject geofence = jsonArray.getJSONObject(i);
                        if (String.valueOf(geofence.getInt(CTGeofenceConstants.KEY_ID))
                                .equals(triggeredGeofence.getRequestId())) {
                            // triggered geofence found in file

                            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                    "Triggered geofence with id = " + triggeredGeofence.getRequestId()
                                            + " is found in file! Sending it to CT SDK");

                            isTriggeredGeofenceFound = true;

                            if (triggeringLocation != null) {
                                geofence.put("triggered_lat", triggeringLocation.getLatitude());
                                geofence.put("triggered_lng", triggeringLocation.getLongitude());
                            }

                            Future<?> future;

                            CleverTapAPI cleverTapApi = CTGeofenceAPI.getInstance(context).getCleverTapApi();

                            if (cleverTapApi == null) {
                                return;
                            }

                            final CTGeofenceEventsListener ctGeofenceEventsListener = CTGeofenceAPI
                                    .getInstance(context).getCtGeofenceEventsListener();

                            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {

                                // send event to CleverTap SDK
                                future = cleverTapApi.pushGeofenceEnteredEvent(geofence);

                                // send event to Listener on main thread
                                if (ctGeofenceEventsListener != null) {
                                    com.clevertap.android.sdk.Utils.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ctGeofenceEventsListener.onGeofenceEnteredEvent(geofence);
                                        }
                                    });
                                }

                            } else {

                                // send event to CleverTap SDK
                                future = cleverTapApi.pushGeoFenceExitedEvent(geofence);

                                // send event to Listener on main thread
                                if (ctGeofenceEventsListener != null) {
                                    com.clevertap.android.sdk.Utils.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ctGeofenceEventsListener.onGeofenceExitedEvent(geofence);
                                        }
                                    });
                                }
                            }

                            try {
                                CTGeofenceAPI.getLogger().verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                        "Calling future for geofence event with id = " +
                                                triggeredGeofence.getRequestId());
                                future.get();

                                CTGeofenceAPI.getLogger().verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                        "Finished calling future for geofence event with id = " +
                                                triggeredGeofence.getRequestId());
                            } catch (Exception e) {
                                CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                        "Failed to push geofence event with id = " +
                                                triggeredGeofence.getRequestId());
                                e.printStackTrace();
                            }

                            break;
                        }
                    }

                    if (!isTriggeredGeofenceFound) {
                        CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                                "Triggered geofence with id = " + triggeredGeofence.getRequestId()
                                        + " is not found in file! Dropping this event");
                        if (CTGeofenceAPI.getInstance(context).getCleverTapApi() != null) {
                            CTGeofenceAPI.getInstance(context)
                                    .getCleverTapApi()
                                    .pushGeoFenceError(CTGeofenceConstants.ERROR_CODE,
                                            "Triggered geofence with id = " +
                                                    triggeredGeofence.getRequestId()
                                                    + " is not found in file! Dropping this event");
                        }
                    }

                }
            } catch (Exception e) {
                CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "Failed to read triggered geofences from file");
                e.printStackTrace();
            }
        }
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
