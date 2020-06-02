package com.clevertap.android.sdk;

import android.content.Context;
import android.content.SharedPreferences;

public final class StorageHelper {

    static void putString(Context context, String key, String value) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit().putString(key, value);
        persist(editor);
    }

    static void removeString(Context context, String key) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit().remove(key);
        persist(editor);
    }

    static String getString(Context context, String key, String defaultValue) {
        return getPreferences(context).getString(key, defaultValue);
    }

    static String getString(Context context, String nameSpace, String key, String defaultValue) {
        return getPreferences(context, nameSpace).getString(key, defaultValue);
    }

    @SuppressWarnings("unused")
    static void putLong(Context context, String key, long value) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit().putLong(key, value);
        persist(editor);
    }

    @SuppressWarnings("unused")
    static long getLong(Context context, String key, long defaultValue) {
        return getPreferences(context).getLong(key, defaultValue);
    }

    static long getLong(Context context, String nameSpace, String key, long defaultValue) {
        return getPreferences(context, nameSpace).getLong(key, defaultValue);
    }

    static void putInt(Context context, String key, int value) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit().putInt(key, value);
        persist(editor);
    }


    static int getInt(Context context, String key, int defaultValue) {
        return getPreferences(context).getInt(key, defaultValue);
    }

    static void putBoolean(Context context, String key, boolean value) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit().putBoolean(key, value);
        persist(editor);
    }

    @SuppressWarnings("SameParameterValue")
    static boolean getBoolean(Context context, String key, boolean defaultValue) {
        return getPreferences(context).getBoolean(key, defaultValue);
    }

    public static SharedPreferences getPreferences(Context context, String namespace) {
        String path = Constants.CLEVERTAP_STORAGE_TAG;

        if (namespace != null) {
            path += "_" + namespace;
        }
        return context.getSharedPreferences(path, Context.MODE_PRIVATE);
    }

    static SharedPreferences getPreferences(Context context) {
        return getPreferences(context, null);
    }

    public static void persist(final SharedPreferences.Editor editor) {
        try {
            editor.apply();
        } catch (Throwable t) {
            Logger.v("CRITICAL: Failed to persist shared preferences!", t);
        }
    }
}
