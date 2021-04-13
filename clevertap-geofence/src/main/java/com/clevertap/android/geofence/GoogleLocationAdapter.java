package com.clevertap.android.geofence;

import static android.app.PendingIntent.FLAG_NO_CREATE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.clevertap.android.geofence.CTGeofenceConstants.TAG_WORK_LOCATION_UPDATES;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.clevertap.android.geofence.interfaces.CTLocationAdapter;
import com.clevertap.android.geofence.interfaces.CTLocationCallback;
import com.clevertap.android.sdk.CleverTapAPI;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.util.concurrent.TimeUnit;

/**
 * Communicates with {@link FusedLocationProviderClient} and {@link WorkManager} to
 * Request/Remove Location updates based on Location fetch modes -
 * ({@link CTGeofenceSettings#FETCH_CURRENT_LOCATION_PERIODIC}) or
 * ({@link CTGeofenceSettings#FETCH_LAST_LOCATION_PERIODIC}) and config settings
 */
class GoogleLocationAdapter implements CTLocationAdapter {

    /**
     * Applicable for both fetch modes ({@link CTGeofenceSettings#FETCH_CURRENT_LOCATION_PERIODIC}) and
     * ({@link CTGeofenceSettings#FETCH_LAST_LOCATION_PERIODIC})
     */
    static final long INTERVAL_IN_MILLIS = 30 * 60 * 1000L;

    /**
     * Applicable only for ({@link CTGeofenceSettings#FETCH_CURRENT_LOCATION_PERIODIC})
     */
    static final long INTERVAL_FASTEST_IN_MILLIS = 30 * 60 * 1000L;

    /**
     * Applicable only for ({@link CTGeofenceSettings#FETCH_CURRENT_LOCATION_PERIODIC})
     */
    static final float SMALLEST_DISPLACEMENT_IN_METERS = 200;

    /**
     * Applicable only for ({@link CTGeofenceSettings#FETCH_LAST_LOCATION_PERIODIC})
     */
    private static final long FLEX_INTERVAL_IN_MILLIS = 10 * 60 * 1000L;

    private boolean backgroundLocationUpdatesEnabled;

    private final Context context;

    private long fastestInterval;

    private final FusedLocationProviderClient fusedProviderClient;

    private long interval;

    /**
     * Applicable only for ({@link CTGeofenceSettings#FETCH_CURRENT_LOCATION_PERIODIC})
     */
    private int locationAccuracy = LocationRequest.PRIORITY_HIGH_ACCURACY;

    private int locationFetchMode;

    private float smallestDisplacement;

    GoogleLocationAdapter(@NonNull Context context) {
        this.context = context.getApplicationContext();
        fusedProviderClient = LocationServices.getFusedLocationProviderClient(this.context);
    }

    /**
     * Gets last known location from {@link FusedLocationProviderClient} and delivers it to caller
     * through {@link CTLocationCallback}
     * <br><br>
     * <b>Must be called from background thread</b>
     *
     * @param callback instance of {@link CTLocationCallback}
     */
    @WorkerThread
    @Override
    public void getLastLocation(@NonNull final CTLocationCallback callback) {
        //thread safe

        CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, "Requesting Last Location..");

        Location location = null;
        try {
            @SuppressLint("MissingPermission")
            Task<Location> lastLocation = fusedProviderClient.getLastLocation();

            // blocking task
            location = Tasks.await(lastLocation);

            if (location != null) {
                CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "New Location = " + location.getLatitude() + "," +
                                location.getLongitude());
            }

            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, "Last location request completed");

        } catch (Exception e) {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Failed to request last location");
            e.printStackTrace();
        } finally {
            callback.onLocationComplete(location);
        }

    }

    /**
     * Helper method to remove location updates for both fetch modes ({@link CTGeofenceSettings#FETCH_CURRENT_LOCATION_PERIODIC})
     * and
     * ({@link CTGeofenceSettings#FETCH_LAST_LOCATION_PERIODIC})
     * <br><br>
     * <b>Must be called from background thread</b>
     *
     * @param pendingIntent instance of {@link PendingIntent} of type
     *                      {@link PendingIntentFactory#PENDING_INTENT_LOCATION}
     */
    @WorkerThread
    @Override
    public void removeLocationUpdates(@Nullable PendingIntent pendingIntent) {
        clearLocationUpdates(pendingIntent);
        clearLocationWorkRequest();
    }

    /**
     * Communicates with {@link FusedLocationProviderClient} and {@link WorkManager} to
     * Request/Remove Location updates based on Location fetch modes -
     * ({@link CTGeofenceSettings#FETCH_CURRENT_LOCATION_PERIODIC}) or
     * ({@link CTGeofenceSettings#FETCH_LAST_LOCATION_PERIODIC}) and config settings
     * <br><br>
     * <b>Must be called from background thread</b>
     */
    @WorkerThread
    @Override
    public void requestLocationUpdates() {
        CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "requestLocationUpdates() called");

        applySettings(context);

        if (!backgroundLocationUpdatesEnabled) {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "not requesting location updates since background location updates is not enabled");
            if (CTGeofenceAPI.getInstance(context).getCleverTapApi() != null) {
                CTGeofenceAPI.getInstance(context)
                        .getCleverTapApi()
                        .pushGeoFenceError(CTGeofenceConstants.ERROR_CODE,
                                "not requesting location updates since background location updates is not enabled");
            }
            return;
        }

        if (locationFetchMode == CTGeofenceSettings.FETCH_CURRENT_LOCATION_PERIODIC) {

            // should get same pendingIntent on each app launch or else instance will leak
            PendingIntent pendingIntent = PendingIntentFactory.getPendingIntent(context,
                    PendingIntentFactory.PENDING_INTENT_LOCATION, FLAG_UPDATE_CURRENT);

            clearLocationWorkRequest();

            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "requesting current location periodically..");

            try {
                // will overwrite location request if change in location config is detected
                @SuppressLint("MissingPermission")
                Task<Void> requestLocationUpdatesTask = fusedProviderClient
                        .requestLocationUpdates(getLocationRequest(), pendingIntent);

                // blocking task
                Tasks.await(requestLocationUpdatesTask);

                CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "Finished requesting current location periodically..");
            } catch (Exception e) {
                CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "Failed to request location updates");
                e.printStackTrace();
            }
        } else {

            // remove previously registered location request
            PendingIntent pendingIntent = PendingIntentFactory.getPendingIntent(context,
                    PendingIntentFactory.PENDING_INTENT_LOCATION, FLAG_NO_CREATE);

            clearLocationUpdates(pendingIntent);

            // start periodic work for location updates
            scheduleManualLocationUpdates();
        }
    }

    /**
     * Retrieves config settings from {@link CTGeofenceSettings} provided by client through
     * {@link CTGeofenceAPI#init(CTGeofenceSettings, CleverTapAPI)}
     *
     * @param context application {@link Context}
     */
    private void applySettings(Context context) {
        CTGeofenceSettings geofenceSettings = CTGeofenceAPI.getInstance(context).getGeofenceSettings();

        if (geofenceSettings == null) {
            geofenceSettings = CTGeofenceAPI.getInstance(context).initDefaultConfig();
        }

        locationFetchMode = geofenceSettings.getLocationFetchMode();
        backgroundLocationUpdatesEnabled = geofenceSettings.isBackgroundLocationUpdatesEnabled();
        interval = geofenceSettings.getInterval();
        fastestInterval = geofenceSettings.getFastestInterval();
        smallestDisplacement = geofenceSettings.getSmallestDisplacement();

        int accuracy = geofenceSettings.getLocationAccuracy();
        switch (accuracy) {
            case 1:
                locationAccuracy = LocationRequest.PRIORITY_HIGH_ACCURACY;
                break;
            case 2:
                locationAccuracy = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
                break;
            case 3:
                locationAccuracy = LocationRequest.PRIORITY_LOW_POWER;
                break;
        }
    }

    /**
     * Removes Location Update of type ({@link CTGeofenceSettings#FETCH_CURRENT_LOCATION_PERIODIC})
     * using {@link FusedLocationProviderClient}
     * <br><br>
     * <b>Must be called from background thread</b>
     */
    @WorkerThread
    private void clearLocationUpdates(@Nullable PendingIntent pendingIntent) {
        if (pendingIntent == null) {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Can't stop location updates since provided pendingIntent is null");
            return;
        }

        try {

            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "removing periodic current location request..");

            Task<Void> removeLocationUpdatesTask = fusedProviderClient.removeLocationUpdates(pendingIntent);

            // blocking task
            Tasks.await(removeLocationUpdatesTask);

            pendingIntent.cancel();

            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Successfully removed periodic current location request");
        } catch (Exception e) {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Failed to remove location updates");
            e.printStackTrace();
        }
    }

    /**
     * Removes Periodic Location Update work request
     */
    private void clearLocationWorkRequest() {

        if (!Utils.isConcurrentFuturesDependencyAvailable()) {
            CTGeofenceAPI.getLogger().info(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "concurrent-futures dependency is missing");
            return;
        }

        try {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "removing periodic last location request..");

            WorkManager.getInstance(context).cancelUniqueWork(TAG_WORK_LOCATION_UPDATES);

            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Successfully removed periodic last location request");

        } catch (NoClassDefFoundError t) {
            CTGeofenceAPI.getLogger().info(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "WorkManager dependency is missing");
        } catch (Throwable t) {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Failed to cancel location work request");
            t.printStackTrace();
        }
    }

    /**
     * Builds an instance of {@link LocationRequest} using config settings
     *
     * @return an instance of {@link LocationRequest}
     */
    private LocationRequest getLocationRequest() {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(interval);
        locationRequest.setFastestInterval(fastestInterval);
        locationRequest.setPriority(locationAccuracy);
        locationRequest.setSmallestDisplacement(smallestDisplacement);

        return locationRequest;
    }

    /**
     * Schedules Periodic Location Update work request using config settings,
     * if work manager dependencies available
     */
    private void scheduleManualLocationUpdates() {

        if (!Utils.isConcurrentFuturesDependencyAvailable()) {
            CTGeofenceAPI.getLogger().info(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "concurrent-futures dependency is missing");
            if (CTGeofenceAPI.getInstance(context).getCleverTapApi() != null) {
                CTGeofenceAPI.getInstance(context)
                        .getCleverTapApi()
                        .pushGeoFenceError(CTGeofenceConstants.ERROR_CODE,
                                "concurrent-futures dependency is missing");
            }
            return;
        }

        CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Scheduling periodic last location request..");

        try {
            PeriodicWorkRequest locationRequest = new PeriodicWorkRequest.Builder(BackgroundLocationWork.class,
                    interval, TimeUnit.MILLISECONDS,
                    FLEX_INTERVAL_IN_MILLIS, TimeUnit.MILLISECONDS)
                    .build();

            // schedule unique work request to avoid duplicates
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(TAG_WORK_LOCATION_UPDATES,
                    ExistingPeriodicWorkPolicy.KEEP, locationRequest);

            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Finished scheduling periodic last location request..");

        } catch (NoClassDefFoundError t) {
            CTGeofenceAPI.getLogger().info(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "WorkManager dependency is missing");
        } catch (Throwable t) {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Failed to request periodic work request");
            t.printStackTrace();
        }
    }

}
