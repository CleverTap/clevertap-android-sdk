package com.clevertap.android.geofence;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.content.ContextCompat;
import com.clevertap.android.geofence.interfaces.CTLocationUpdatesListener;
import com.clevertap.android.sdk.CleverTapAPI;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class Utils {

    private static Boolean isPlayServicesDependencyAvailable;

    private static Boolean isFusedLocationDependencyAvailable;

    private static Boolean isConcurrentFuturesDependencyAvailable;

    static String emptyIfNull(String str) {
        return str == null ? "" : str;
    }

    static int getGeofenceSDKVersion() {
        return BuildConfig.VERSION_CODE;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean hasBackgroundLocationPermission(final Context context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                return hasPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
            } else {
                return true;
            }
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Checks if Application has provided permission
     *
     * @param context    application {@link Context}
     * @param permission for example, {@link Manifest.permission#ACCESS_FINE_LOCATION}
     */
    static boolean hasPermission(final Context context, String permission) {
        try {
            return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, permission);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Creates {@link com.clevertap.android.sdk.CleverTapAPI} instance if it's null and initializes
     * Geofence SDK, mostly in killed state.
     * <br>
     * <b>Must be called from background thread</b>
     *
     * @param context application {@link Context}
     * @return true if geofence sdk initialized successfully, false otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    @WorkerThread
    static boolean initCTGeofenceApiIfRequired(@NonNull Context context) {

        CTGeofenceAPI ctGeofenceAPI = CTGeofenceAPI.getInstance(context);

        if (ctGeofenceAPI.getCleverTapApi() == null) {
            CTGeofenceSettings ctGeofenceSettings = Utils.readSettingsFromFile(context);
            if (ctGeofenceSettings == null) {
                CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "Could not initialize CT instance! Dropping this call");
                return false;
            }

            CleverTapAPI cleverTapAPI = CleverTapAPI.getGlobalInstance(context, ctGeofenceSettings.getId());

            if (cleverTapAPI == null) {
                CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "Critical issue :: After calling  CleverTapAPI.getGlobalInstance also init is failed! Dropping this call");
                return false;
            }

            ctGeofenceAPI.init(ctGeofenceSettings, cleverTapAPI);
        }

        return true;
    }

    /**
     * Checks if Google Play services dependency is available.
     *
     * @return <code>true</code> if available, otherwise <code>false</code>.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean isConcurrentFuturesDependencyAvailable() {

        if (isConcurrentFuturesDependencyAvailable == null) {//use reflection only once
            // concurrent futures
            try {
                Class.forName("androidx.concurrent.futures.CallbackToFutureAdapter");
                isConcurrentFuturesDependencyAvailable = true;
            } catch (ClassNotFoundException e) {
                isConcurrentFuturesDependencyAvailable = false;
            }
        }

        return isConcurrentFuturesDependencyAvailable;
    }

    /**
     * Checks if Google Play services dependency is available for Fused Location.
     *
     * @return <code>true</code> if available, otherwise <code>false</code>.
     */
    static boolean isFusedLocationApiDependencyAvailable() {

        if (isFusedLocationDependencyAvailable == null) {//use reflection only once
            if (!isPlayServicesDependencyAvailable()) {
                isFusedLocationDependencyAvailable = false;
            } else {
                try {
                    Class.forName("com.google.android.gms.location.FusedLocationProviderClient");
                    isFusedLocationDependencyAvailable = true;
                } catch (ClassNotFoundException e) {
                    isFusedLocationDependencyAvailable = false;
                }
            }
        }

        return isFusedLocationDependencyAvailable;
    }

    /**
     * Converts {@link JSONObject} to list of {@link com.google.android.gms.location.Geofence} Ids
     *
     * @param jsonObject containing geofence list
     * @return list of {@link com.google.android.gms.location.Geofence} Ids
     */
    static List<String> jsonToGeoFenceList(@NonNull JSONObject jsonObject) {
        ArrayList<String> geofenceIdList = new ArrayList<>();
        try {
            JSONArray array = jsonObject.getJSONArray("geofences");

            for (int i = 0; i < array.length(); i++) {

                JSONObject object = array.getJSONObject(i);
                geofenceIdList.add(object.getString(CTGeofenceConstants.KEY_ID));
            }
        } catch (Exception e) {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Could not convert JSON to GeofenceIdList - " + e.getMessage());
            e.printStackTrace();
        }
        return geofenceIdList;
    }

    /**
     * Notifies Listener for location update on main thread through {@link CTLocationUpdatesListener}
     *
     * @param context  application {@link Context}
     * @param location instance of {@link Location}
     */
    static void notifyLocationUpdates(@NonNull Context context, @Nullable final Location location) {
        final CTLocationUpdatesListener ctLocationUpdatesListener = CTGeofenceAPI.getInstance(context)
                .getCtLocationUpdatesListener();

        if (ctLocationUpdatesListener != null) {
            com.clevertap.android.sdk.Utils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ctLocationUpdatesListener.onLocationUpdates(location);
                }
            });
        }
    }

    /**
     * Reads {@link CTGeofenceSettings} from file.
     * <br>
     * <b>Must be called from background thread</b>
     *
     * @param context application {@link Context}
     * @return {@link CTGeofenceSettings}, null if no settings found in file
     */
    @WorkerThread
    @Nullable
    static CTGeofenceSettings readSettingsFromFile(@NonNull Context context) {

        CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Reading settings from file...");

        CTGeofenceSettings ctGeofenceSettings = null;

        String settingsString = FileUtils.readFromFile(context,
                FileUtils.getCachedFullPath(context, CTGeofenceConstants.SETTINGS_FILE_NAME));
        if (!settingsString.trim().equals("")) {
            try {
                JSONObject jsonObject = new JSONObject(settingsString);

                ctGeofenceSettings = new CTGeofenceSettings.Builder()
                        .enableBackgroundLocationUpdates(
                                jsonObject.getBoolean(CTGeofenceConstants.KEY_LAST_BG_LOCATION_UPDATES))
                        .setLocationAccuracy((byte) jsonObject.getInt(CTGeofenceConstants.KEY_LAST_ACCURACY))
                        .setLocationFetchMode((byte) jsonObject.getInt(CTGeofenceConstants.KEY_LAST_FETCH_MODE))
                        .setLogLevel(jsonObject.getInt(CTGeofenceConstants.KEY_LAST_LOG_LEVEL))
                        .setGeofenceMonitoringCount(jsonObject.getInt(CTGeofenceConstants.KEY_LAST_GEO_COUNT))
                        .setId(jsonObject.getString(CTGeofenceConstants.KEY_ID))
                        .setInterval(jsonObject.getLong(CTGeofenceConstants.KEY_LAST_INTERVAL))
                        .setFastestInterval(jsonObject.getLong(CTGeofenceConstants.KEY_LAST_FASTEST_INTERVAL))
                        .setSmallestDisplacement(
                                (float) jsonObject.getDouble(CTGeofenceConstants.KEY_LAST_DISPLACEMENT))
                        .setGeofenceNotificationResponsiveness(
                                jsonObject.getInt(CTGeofenceConstants.KEY_LAST_GEO_NOTIFICATION_RESPONSIVENESS))
                        .build();

                CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "Read settings successfully from file");

            } catch (Exception e) {
                CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "Failed to read geofence settings from file");
            }
        } else {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Settings not found in file...");
        }

        return ctGeofenceSettings;

    }

    /**
     * Creates sub array from provided {@link JSONArray}
     *
     * @param arr       {@link JSONArray}
     * @param fromIndex fromIndex
     * @param toIndex   toIndex
     * @return sub array exclusive of toIndex
     * @throws IllegalStateException if fromIndex > toIndex
     */
    @NonNull
    static JSONArray subArray(@NonNull JSONArray arr, int fromIndex, int toIndex) {

        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }

        JSONArray jsonArray = new JSONArray();

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                jsonArray.put(arr.getJSONObject(i));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonArray;

    }

    /**
     * Writes {@link CTGeofenceSettings} to file.
     * <br>
     * <b>Must be called from background thread</b>
     *
     * @param context            application {@link Context}
     * @param ctGeofenceSettings new {@link CTGeofenceSettings}
     */
    @WorkerThread
    static void writeSettingsToFile(Context context, @NonNull CTGeofenceSettings ctGeofenceSettings) {

        CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                "Writing new settings to file...");

        JSONObject settings = new JSONObject();
        try {
            settings.put(CTGeofenceConstants.KEY_LAST_ACCURACY, ctGeofenceSettings.getLocationAccuracy());
            settings.put(CTGeofenceConstants.KEY_LAST_FETCH_MODE, ctGeofenceSettings.getLocationFetchMode());
            settings.put(CTGeofenceConstants.KEY_LAST_BG_LOCATION_UPDATES,
                    ctGeofenceSettings.isBackgroundLocationUpdatesEnabled());
            settings.put(CTGeofenceConstants.KEY_LAST_LOG_LEVEL, ctGeofenceSettings.getLogLevel());
            settings.put(CTGeofenceConstants.KEY_LAST_GEO_COUNT, ctGeofenceSettings.getGeofenceMonitoringCount());
            settings.put(CTGeofenceConstants.KEY_LAST_INTERVAL, ctGeofenceSettings.getInterval());
            settings.put(CTGeofenceConstants.KEY_LAST_FASTEST_INTERVAL, ctGeofenceSettings.getFastestInterval());
            settings.put(CTGeofenceConstants.KEY_LAST_DISPLACEMENT, ctGeofenceSettings.getSmallestDisplacement());
            settings.put(CTGeofenceConstants.KEY_LAST_GEO_NOTIFICATION_RESPONSIVENESS,
                    ctGeofenceSettings.getGeofenceNotificationResponsiveness());
            settings.put(CTGeofenceConstants.KEY_ID, CTGeofenceAPI.getInstance(context).getAccountId());

            boolean writeJsonToFile = FileUtils.writeJsonToFile(context, FileUtils.getCachedDirName(context),
                    CTGeofenceConstants.SETTINGS_FILE_NAME, settings);

            if (writeJsonToFile) {
                CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "New settings successfully written to file");
            } else {
                CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                        "Failed to write new settings to file");
            }

        } catch (JSONException e) {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Failed to write new settings to file while parsing json");
        } catch (Exception e) {
            CTGeofenceAPI.getLogger().debug(CTGeofenceAPI.GEOFENCE_LOG_TAG,
                    "Failed to write new settings to file");
        }

    }

    /**
     * Checks if Google Play services dependency is available.
     *
     * @return <code>true</code> if available, otherwise <code>false</code>.
     */
    private static boolean isPlayServicesDependencyAvailable() {

        if (isPlayServicesDependencyAvailable == null) {//use reflection only once
            // Play Services
            try {
                Class.forName("com.google.android.gms.common.GooglePlayServicesUtil");
                isPlayServicesDependencyAvailable = true;
            } catch (ClassNotFoundException e) {
                isPlayServicesDependencyAvailable = false;
            }
        }

        return isPlayServicesDependencyAvailable;
    }
}
