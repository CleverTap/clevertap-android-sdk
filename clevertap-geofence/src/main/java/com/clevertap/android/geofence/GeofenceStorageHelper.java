package com.clevertap.android.geofence;

import android.content.Context;
import android.content.SharedPreferences;

public final class GeofenceStorageHelper {

    static double getDouble(Context context, String key, double defaultValue) {
        SharedPreferences prefs = getPreferences(context);
        if (!prefs.contains(key)) {
            return defaultValue;
        }

        return Double.longBitsToDouble(prefs.getLong(key, 0));
    }

    static long getLong(Context context, String key, long defaultValue) {
        return getPreferences(context).getLong(key, defaultValue);
    }

    static SharedPreferences getPreferences(Context context, String namespace) {
        String path = CTGeofenceConstants.GEOFENCE_PREF_STORAGE_TAG;

        if (namespace != null) {
            path += "_" + namespace;
        }
        return context.getSharedPreferences(path, Context.MODE_PRIVATE);
    }

    static SharedPreferences getPreferences(Context context) {
        return getPreferences(context, null);
    }

    static void persist(final SharedPreferences.Editor editor) {
        try {
            editor.commit();
        } catch (Throwable t) {
            CTGeofenceAPI.getLogger()
                    .debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, "CRITICAL: Failed to persist shared preferences!");
        }
    }

    static void putDouble(Context context, String key, double value) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit().putLong(key, Double.doubleToRawLongBits(value));
        persist(editor);
    }

    static void putLong(Context context, String key, long value) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit().putLong(key, value);
        persist(editor);
    }
}