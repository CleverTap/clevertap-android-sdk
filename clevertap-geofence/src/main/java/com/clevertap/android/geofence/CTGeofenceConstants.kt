package com.clevertap.android.geofence

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object CTGeofenceConstants {

    const val KEY_GEOFENCES: String = "geofences"

    const val KEY_ID: String = "id"

    const val GEOFENCE_PREF_STORAGE_TAG: String = "com.clevertap.android.geofence.geofence_pref"

    const val KEY_LATITUDE: String = "latitude"

    const val KEY_LONGITUDE: String = "longitude"

    const val KEY_LAST_LOCATION_EP: String = "last_location_ep"

    const val CACHED_DIR_NAME: String = "geofence"

    const val CACHED_FILE_NAME: String = "geofence_cache.json"

    const val SETTINGS_FILE_NAME: String = "geofence_settings.json"

    const val ACTION_GEOFENCE_RECEIVER: String = "com.clevertap.android.geofence.fence.update"

    const val ACTION_LOCATION_RECEIVER: String = "com.clevertap.android.geofence.location.update"

    const val KEY_LAST_ACCURACY: String = "last_accuracy"

    const val KEY_LAST_FETCH_MODE: String = "last_fetch_mode"

    const val KEY_LAST_BG_LOCATION_UPDATES: String = "last_bg_location_updates"

    const val KEY_LAST_LOG_LEVEL: String = "last_log_level"

    const val KEY_LAST_GEO_COUNT: String = "last_geo_count"

    const val KEY_LAST_INTERVAL: String = "last_interval"

    const val KEY_LAST_FASTEST_INTERVAL: String = "last_fastest_interval"

    const val KEY_LAST_DISPLACEMENT: String = "last_displacement"

    const val KEY_LAST_GEO_NOTIFICATION_RESPONSIVENESS: String = "last_geo_notification_responsiveness"

    const val TAG_WORK_LOCATION_UPDATES: String = "com.clevertap.android.geofence.work.location"

    const val ERROR_CODE: Int = 515

    const val DEFAULT_LATITUDE: Double = 2.189866

    const val DEFAULT_LONGITUDE: Double = 70.900955
}
