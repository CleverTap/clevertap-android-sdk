package com.clevertap.android.geofence;

public class CTGeofenceConstants {

    public static final String KEY_GEOFENCES = "geofences";

    public static final String KEY_ID = "id";

    static final String GEOFENCE_PREF_STORAGE_TAG = "com.clevertap.android.geofence.geofence_pref";

    static final String KEY_LATITUDE = "latitude";

    static final String KEY_LONGITUDE = "longitude";

    static final String KEY_LAST_LOCATION_EP = "last_location_ep";

    static final String CACHED_DIR_NAME = "geofence";

    static final String CACHED_FILE_NAME = "geofence_cache.json";

    static final String SETTINGS_FILE_NAME = "geofence_settings.json";

    static final String ACTION_GEOFENCE_RECEIVER = "com.clevertap.android.geofence.fence.update";

    static final String ACTION_LOCATION_RECEIVER = "com.clevertap.android.geofence.location.update";

    static final String KEY_LAST_ACCURACY = "last_accuracy";

    static final String KEY_LAST_FETCH_MODE = "last_fetch_mode";

    static final String KEY_LAST_BG_LOCATION_UPDATES = "last_bg_location_updates";

    static final String KEY_LAST_LOG_LEVEL = "last_log_level";

    static final String KEY_LAST_GEO_COUNT = "last_geo_count";

    static final String KEY_LAST_INTERVAL = "last_interval";

    static final String KEY_LAST_FASTEST_INTERVAL = "last_fastest_interval";

    static final String KEY_LAST_DISPLACEMENT = "last_displacement";

    static final String KEY_LAST_GEO_NOTIFICATION_RESPONSIVENESS = "last_geo_notification_responsiveness";

    static final String TAG_WORK_LOCATION_UPDATES = "com.clevertap.android.geofence.work.location";

    static final int ERROR_CODE = 515;

    static final double DEFAULT_LATITUDE = 2.189866;

    static final double DEFAULT_LONGITUDE = 70.900955;
}
