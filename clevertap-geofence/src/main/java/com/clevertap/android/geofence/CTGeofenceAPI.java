package com.clevertap.android.geofence;

import static android.app.PendingIntent.FLAG_NO_CREATE;
import static com.clevertap.android.geofence.CTGeofenceConstants.DEFAULT_LATITUDE;
import static com.clevertap.android.geofence.CTGeofenceConstants.DEFAULT_LONGITUDE;
import static com.clevertap.android.geofence.GoogleLocationAdapter.INTERVAL_IN_MILLIS;
import static com.clevertap.android.geofence.GoogleLocationAdapter.SMALLEST_DISPLACEMENT_IN_METERS;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import com.clevertap.android.geofence.interfaces.CTGeofenceAdapter;
import com.clevertap.android.geofence.interfaces.CTGeofenceEventsListener;
import com.clevertap.android.geofence.interfaces.CTGeofenceTask;
import com.clevertap.android.geofence.interfaces.CTLocationAdapter;
import com.clevertap.android.geofence.interfaces.CTLocationCallback;
import com.clevertap.android.geofence.interfaces.CTLocationUpdatesListener;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.GeofenceCallback;
import java.util.concurrent.Future;
import org.json.JSONObject;

/**
 * Main Entry point for communicating with Geofence SDK.<br>
 * Singleton class Responsible for initializing, activating and deactivating geofence sdk as
 * requested by applications. It implements {@link GeofenceCallback} to communicate with CleverTap SDK
 * for location pings and update of new geofence list.
 */
public class CTGeofenceAPI implements GeofenceCallback {

    public interface OnGeofenceApiInitializedListener {

        void OnGeofenceApiInitialized();
    }

    public static final String GEOFENCE_LOG_TAG = "CTGeofence";

    private static CTGeofenceAPI ctGeofenceAPI;

    private static final Logger logger;

    private String accountId;

    @Nullable
    private CleverTapAPI cleverTapAPI;

    private final Context context;

    @Nullable
    private CTGeofenceAdapter ctGeofenceAdapter;

    @Nullable
    private CTGeofenceEventsListener ctGeofenceEventsListener;

    @Nullable
    private CTGeofenceSettings ctGeofenceSettings;

    @Nullable
    private CTLocationAdapter ctLocationAdapter;

    @Nullable
    private CTLocationUpdatesListener ctLocationUpdatesListener;

    private boolean isActivated;

    @Nullable
    private OnGeofenceApiInitializedListener onGeofenceApiInitializedListener;

    /**
     * Retrieves the {@code default} singleton instance of {@link CTGeofenceAPI}.
     *
     * @param context A {@link Context} for Application
     * @return The singleton instance of {@link CTGeofenceAPI}
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public static synchronized CTGeofenceAPI getInstance(@NonNull Context context) {
        if (ctGeofenceAPI == null) {
            ctGeofenceAPI = new CTGeofenceAPI(context);
        }
        return ctGeofenceAPI;
    }

    public static Logger getLogger() {
        return logger;
    }

    private CTGeofenceAPI(Context context) {
        this.context = context.getApplicationContext();

        try {
            ctLocationAdapter = CTLocationFactory.createLocationAdapter(this.context);
            ctGeofenceAdapter = CTGeofenceFactory.createGeofenceAdapter(this.context);
        } catch (IllegalStateException e) {
            if (e.getMessage() != null) {
                CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        e.getMessage());
            }
        }
    }

    /**
     * Unregisters geofences, background location updates and clears file storage and preferences<br>
     * This operation will be sent to queue and get's executed by background thread using
     * {@link CTGeofenceTaskManager}
     */
    @SuppressWarnings("unused")
    public void deactivate() {

        if (ctLocationAdapter == null || ctGeofenceAdapter == null) {
            return;
        }

        CTGeofenceTaskManager.getInstance().postAsyncSafely("DeactivateApi", new Runnable() {
            @Override
            public void run() {
                // stop geofence monitoring
                PendingIntent geofencePendingIntent = PendingIntentFactory.getPendingIntent(context,
                        PendingIntentFactory.PENDING_INTENT_GEOFENCE, FLAG_NO_CREATE);
                ctGeofenceAdapter.stopGeofenceMonitoring(geofencePendingIntent);

                // stop location updates
                PendingIntent locationPendingIntent = PendingIntentFactory.getPendingIntent(context,
                        PendingIntentFactory.PENDING_INTENT_LOCATION, FLAG_NO_CREATE);
                ctLocationAdapter.removeLocationUpdates(locationPendingIntent);

                // delete cached files
                FileUtils.deleteDirectory(context, FileUtils.getCachedDirName(context));

                // reset preference
                GeofenceStorageHelper.putDouble(context
                        , CTGeofenceConstants.KEY_LATITUDE, DEFAULT_LATITUDE);
                GeofenceStorageHelper.putDouble(context
                        , CTGeofenceConstants.KEY_LONGITUDE, DEFAULT_LONGITUDE);
                GeofenceStorageHelper.putLong(context
                        , CTGeofenceConstants.KEY_LAST_LOCATION_EP, 0);

                isActivated = false;
            }
        });


    }

    @Nullable
    public CTGeofenceEventsListener getCtGeofenceEventsListener() {
        return ctGeofenceEventsListener;
    }

    public void setCtGeofenceEventsListener(@NonNull CTGeofenceEventsListener ctGeofenceEventsListener) {
        this.ctGeofenceEventsListener = ctGeofenceEventsListener;
    }

    @Nullable
    public CTLocationUpdatesListener getCtLocationUpdatesListener() {
        return ctLocationUpdatesListener;
    }

    public void setCtLocationUpdatesListener(@NonNull CTLocationUpdatesListener ctLocationUpdatesListener) {
        this.ctLocationUpdatesListener = ctLocationUpdatesListener;
    }

    @SuppressWarnings("WeakerAccess")
    public @Nullable
    CTGeofenceSettings getGeofenceSettings() {
        return ctGeofenceSettings;
    }

    /**
     * Sets geofence API configuration settings with provided {@link CTGeofenceSettings}
     *
     * @param ctGeofenceSettings instance of {@link CTGeofenceSettings}
     */
    @SuppressWarnings("unused")
    private void setGeofenceSettings(CTGeofenceSettings ctGeofenceSettings) {

        if (this.ctGeofenceSettings != null) {
            logger.verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Settings already configured");
            return;
        }

        this.ctGeofenceSettings = ctGeofenceSettings;
    }

    /**
     * Creates {@link GeofenceUpdateTask}(to register list of geofences) and sends it to Queue using
     * {@link CTGeofenceTaskManager} if application has {@link Manifest.permission#ACCESS_FINE_LOCATION}
     * and {@link Manifest.permission#ACCESS_BACKGROUND_LOCATION} permissions.<br>
     * This method is for internal usage only, apps must not call this externally.
     *
     * @param fenceList instance of {@link JSONObject} containing geofence list to register to OS
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Override
    public void handleGeoFences(JSONObject fenceList) {

        if (ctLocationAdapter == null || ctGeofenceAdapter == null) {
            return;
        }

        if (!Utils.hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            logger.debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "We don't have ACCESS_FINE_LOCATION permission! dropping geofence update call");
            if (this.cleverTapAPI != null) {
                this.cleverTapAPI.pushGeoFenceError(CTGeofenceConstants.ERROR_CODE,
                        "We don't have ACCESS_FINE_LOCATION permission! Dropping initBackgroundLocationUpdates() call");
            }
            return;
        }

        if (!Utils.hasBackgroundLocationPermission(context)) {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "We don't have ACCESS_BACKGROUND_LOCATION permission! dropping geofence update call");
            if (this.cleverTapAPI != null) {
                this.cleverTapAPI.pushGeoFenceError(CTGeofenceConstants.ERROR_CODE,
                        "We don't have ACCESS_BACKGROUND_LOCATION permission! dropping geofence update call");
            }
            return;
        }

        if (fenceList == null) {
            logger.debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Geofence response is null! dropping further processing");
            return;
        }

        GeofenceUpdateTask geofenceUpdateTask = new GeofenceUpdateTask(context, fenceList);

        CTGeofenceTaskManager.getInstance().postAsyncSafely("ProcessGeofenceUpdates",
                geofenceUpdateTask);
    }

    /**
     * Initializes and activates SDK with provided {@link CTGeofenceSettings} and {@link CleverTapAPI}
     * instance.
     *
     * @param ctGeofenceSettings instance of {@link CTGeofenceSettings}.Can be null in which case
     *                           default settings will be applied.
     * @param cleverTapAPI       NonNull instance of  {@link CleverTapAPI}.
     */
    public void init(CTGeofenceSettings ctGeofenceSettings, @NonNull CleverTapAPI cleverTapAPI) {

        if (ctLocationAdapter == null || ctGeofenceAdapter == null) {
            return;
        }

        setCleverTapApi(cleverTapAPI);
        setGeofenceSettings(ctGeofenceSettings);
        setAccountId(cleverTapAPI.getAccountId());
        activate();
    }

    /**
     * Creates {@link LocationUpdateTask}(to register location updates) and sends it to Queue using {@link
     * CTGeofenceTaskManager}
     * if application has {@link Manifest.permission#ACCESS_FINE_LOCATION} permission. SDK initialization
     * callback({@link OnGeofenceApiInitializedListener#OnGeofenceApiInitialized()}) will be raised as soon as
     * {@link LocationUpdateTask} finishes it's execution.
     *
     * @throws IllegalStateException if Geofence SDK is not initialized before calling this method
     */
    @SuppressWarnings("WeakerAccess")
    public void initBackgroundLocationUpdates() {

        if (ctLocationAdapter == null || ctGeofenceAdapter == null) {
            return;
        }

        if (!Utils.hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            logger.debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "We don't have ACCESS_FINE_LOCATION permission! Dropping initBackgroundLocationUpdates() call");
            if (this.cleverTapAPI != null) {
                this.cleverTapAPI.pushGeoFenceError(CTGeofenceConstants.ERROR_CODE,
                        "We don't have ACCESS_FINE_LOCATION permission! Dropping initBackgroundLocationUpdates() call");
            }
            return;
        }

        logger.debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "requestBackgroundLocationUpdates() called");

        if (!isActivated) {
            throw new IllegalStateException(
                    "Geofence SDK must be initialized before initBackgroundLocationUpdates()");
        }

        LocationUpdateTask locationUpdateTask = new LocationUpdateTask(context);
        locationUpdateTask.setOnCompleteListener(new CTGeofenceTask.OnCompleteListener() {
            @Override
            public void onComplete() {

                if (onGeofenceApiInitializedListener != null) {
                    com.clevertap.android.sdk.Utils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            onGeofenceApiInitializedListener.OnGeofenceApiInitialized();
                        }
                    });
                }
            }
        });

        CTGeofenceTaskManager.getInstance().postAsyncSafely("IntitializeLocationUpdates",
                locationUpdateTask);
    }

    /**
     * Sets {@link OnGeofenceApiInitializedListener} for Geofence SDK initialized callback on main thread
     *
     * @param onGeofenceApiInitializedListener instance of {@link OnGeofenceApiInitializedListener}
     */
    @SuppressWarnings("unused")
    public void setOnGeofenceApiInitializedListener(
            @NonNull OnGeofenceApiInitializedListener onGeofenceApiInitializedListener) {
        this.onGeofenceApiInitializedListener = onGeofenceApiInitializedListener;
    }

    /**
     * Fetches last known location from OS and delivers it to APP through {@link CTLocationUpdatesListener}
     * if application has {@link Manifest.permission#ACCESS_FINE_LOCATION} permission.<br>
     * Fetched Location will be passed to {@link #processTriggeredLocation(Location)} to send it to
     * server for latest geofence list
     *
     * @throws IllegalStateException if Geofence SDK is not initialized before calling this method
     */
    @SuppressWarnings("unused")
    @Override
    public void triggerLocation() {

        if (ctLocationAdapter == null || ctGeofenceAdapter == null || cleverTapAPI == null) {
            return;
        }

        logger.debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "triggerLocation() called");

        if (!Utils.hasPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            logger.debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "We don't have ACCESS_FINE_LOCATION permission! Dropping triggerLocation() call");
            if (this.cleverTapAPI != null) {
                this.cleverTapAPI.pushGeoFenceError(CTGeofenceConstants.ERROR_CODE,
                        "We don't have ACCESS_FINE_LOCATION permission! Dropping triggerLocation() call");
            }
            return;
        }

        if (!isActivated) {
            throw new IllegalStateException("Geofence SDK must be initialized before triggerLocation()");
        }

        CTGeofenceTaskManager.getInstance().postAsyncSafely("TriggerLocation",
                new Runnable() {
                    @Override
                    public void run() {
                        ctLocationAdapter.getLastLocation(new CTLocationCallback() {
                            @Override
                            public void onLocationComplete(Location location) {
                                //get's called on bg thread

                                if (location != null) {
                                    processTriggeredLocation(location);
                                }

                                Utils.notifyLocationUpdates(context, location);
                            }
                        });
                    }
                });


    }

    @NonNull
    String getAccountId() {
        return Utils.emptyIfNull(accountId);
    }

    /**
     * Sets CleverTap account id which will be used by SDK to recreate {@link CleverTapAPI}
     * instance when an app is in killed state. Error will be sent to CleverTap in case null or empty.
     *
     * @param accountId CleverTap account id
     */
    @SuppressWarnings("unused")
    private void setAccountId(String accountId) {

        if (accountId == null || accountId.isEmpty()) {
            logger.debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Account Id is null or empty");
            if (this.cleverTapAPI != null) {
                this.cleverTapAPI.pushGeoFenceError(CTGeofenceConstants.ERROR_CODE, "Account Id is null or empty");
            }
            return;
        }

        this.accountId = accountId;
    }

    @Nullable
    CleverTapAPI getCleverTapApi() {
        return cleverTapAPI;
    }

    /**
     * Sets {@link CleverTapAPI} instance to be used by SDK
     *
     * @param cleverTapAPI instance of {@link CleverTapAPI}
     */
    @SuppressWarnings("unused")
    private void setCleverTapApi(CleverTapAPI cleverTapAPI) {
        this.cleverTapAPI = cleverTapAPI;
    }

    @Nullable
    CTGeofenceAdapter getCtGeofenceAdapter() {
        return ctGeofenceAdapter;
    }

    @Nullable
    CTLocationAdapter getCtLocationAdapter() {
        return ctLocationAdapter;
    }

    /**
     * Creates default instance of {@link CTGeofenceSettings}
     *
     * @return default instance of {@link CTGeofenceSettings}
     */
    @NonNull
    CTGeofenceSettings initDefaultConfig() {
        return new CTGeofenceSettings.Builder().build();
    }

    boolean isActivated() {
        return isActivated;
    }

    /**
     * Sends Location to CleverTap SDK to send it to server with throttling limit of {@code minimum
     * 30 minutes} and {@code minimum displacement of 200 meters} between two location pings.<br>
     * Throttling logic is determined by comparing last pinged location and current one using
     * shared preferences
     *
     * @param location instance of {@link Location}, must be nonnull
     * @return a Future representing pending completion of the task of sending location to server,
     * can be null if CleverTap SDK decides not to send it to server
     */
    @Nullable
    Future<?> processTriggeredLocation(@NonNull Location location) {
        Future<?> future = null;

        try {
            if (cleverTapAPI == null) {
                return null;
            }

            Location lastStoredLocation = new Location("");

            lastStoredLocation.setLatitude(GeofenceStorageHelper
                    .getDouble(context, CTGeofenceConstants.KEY_LATITUDE, DEFAULT_LATITUDE));
            lastStoredLocation.setLongitude(GeofenceStorageHelper
                    .getDouble(context, CTGeofenceConstants.KEY_LONGITUDE, DEFAULT_LONGITUDE));

            long lastStoredLocationEPMillis = GeofenceStorageHelper.getLong(context
                    , CTGeofenceConstants.KEY_LAST_LOCATION_EP, 0);
            long nowMillis = System.currentTimeMillis();

            long deltaT = nowMillis - lastStoredLocationEPMillis;
            float deltaD = location.distanceTo(lastStoredLocation);

            logger.debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Delta T for last two locations = " + deltaT);
            logger.debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Delta D for last two locations = " + deltaD);

            if (deltaT > INTERVAL_IN_MILLIS && deltaD > SMALLEST_DISPLACEMENT_IN_METERS) {

                logger.debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "Sending last location to CleverTap..");

                future = cleverTapAPI.setLocationForGeofences(location, Utils.getGeofenceSDKVersion());

                GeofenceStorageHelper.putDouble(context
                        , CTGeofenceConstants.KEY_LATITUDE, location.getLatitude());
                GeofenceStorageHelper.putDouble(context
                        , CTGeofenceConstants.KEY_LONGITUDE, location.getLongitude());
                GeofenceStorageHelper.putLong(context
                        , CTGeofenceConstants.KEY_LAST_LOCATION_EP, System.currentTimeMillis());

            } else {
                logger.debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "Not sending last location to CleverTap");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return future;
    }

    /**
     * Activates SDK by registering {@link GeofenceCallback}
     * and background location updates on background thread by
     * reading config settings if provided or will use default settings.
     */
    @SuppressWarnings("unused")
    private void activate() {

        if (ctLocationAdapter == null || ctGeofenceAdapter == null || cleverTapAPI == null) {
            return;
        }

        if (isActivated) {
            logger.verbose(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Geofence API already activated! dropping activate() call");
            return;
        }

        if (ctGeofenceSettings == null) {
            ctGeofenceSettings = initDefaultConfig();
        }

        logger.setDebugLevel(ctGeofenceSettings.getLogLevel());

        cleverTapAPI.setGeofenceCallback(this);
        logger.debug(GEOFENCE_LOG_TAG, "geofence callback registered");

        isActivated = true;
        initBackgroundLocationUpdates();
    }

    static {
        logger = new Logger(Logger.DEBUG);
    }
}
