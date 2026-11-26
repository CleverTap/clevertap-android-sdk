package com.clevertap.android.geofence

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object GeofenceStorageHelper {

    @JvmStatic
    fun getDouble(context: Context, key: String?, defaultValue: Double): Double {
        val prefs = getPreferences(context)
        if (!prefs.contains(key)) {
            return defaultValue
        }

        return java.lang.Double.longBitsToDouble(prefs.getLong(key, 0))
    }

    @JvmStatic
    fun getLong(context: Context, key: String?, defaultValue: Long): Long {
        return getPreferences(context).getLong(key, defaultValue)
    }

    @JvmStatic
    fun getPreferences(context: Context, namespace: String?): SharedPreferences {
        var path = CTGeofenceConstants.GEOFENCE_PREF_STORAGE_TAG

        if (namespace != null) {
            path += "_$namespace"
        }
        return context.getSharedPreferences(path, Context.MODE_PRIVATE)
    }

    @JvmStatic
    fun getPreferences(context: Context): SharedPreferences {
        return getPreferences(context, null)
    }

    @JvmStatic
    fun persist(editor: SharedPreferences.Editor) {
        try {
            editor.commit()
        } catch (t: Throwable) {
            CTGeofenceAPI.getLogger()
                .debug(CTGeofenceAPI.GEOFENCE_LOG_TAG, "CRITICAL: Failed to persist shared preferences!")
        }
    }

    @JvmStatic
    fun putDouble(context: Context, key: String?, value: Double) {
        val prefs = getPreferences(context)
        val editor = prefs.edit().putLong(key, java.lang.Double.doubleToRawLongBits(value))
        persist(editor)
    }

    @JvmStatic
    fun putLong(context: Context, key: String?, value: Long) {
        val prefs = getPreferences(context)
        val editor = prefs.edit().putLong(key, value)
        persist(editor)
    }
}
