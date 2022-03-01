package com.clevertap.android.directcall;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;

import com.clevertap.android.directcall.init.DirectCallAPI;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class StorageHelper {

    public static SharedPreferences getPreferences(Context context) {
        String path = Constants.DirectCall_STORAGE_TAG;
        return context.getSharedPreferences(path, Context.MODE_PRIVATE);
    }

    public static void persist(final SharedPreferences.Editor editor) {
        try {
            editor.apply();
        } catch (Throwable t) {
            DirectCallAPI.getLogger().verbose("CRITICAL: Failed to persist shared preferences!", t);
        }
    }

    /**
     * Use this method, when you are sure that you are on background thread
     */
    @WorkerThread
    public static void persistImmediately(final SharedPreferences.Editor editor) {
        try {
            editor.commit();
        } catch (Throwable t) {
            DirectCallAPI.getLogger().verbose("CRITICAL: Failed to persist shared preferences!", t);
        }
    }

    public static int getInt(Context context, String key, int defaultValue) {
        return getPreferences(context).getInt(key, defaultValue);
    }

    public static void putIntImmediate(Context context, String key, int value) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit().putInt(key, value);
        persistImmediately(editor);
    }

    public static String getString(Context context, String key, String defaultValue) {
        return getPreferences(context).getString(key, defaultValue);
    }

    public static void putString(Context context, String key, String value) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit().putString(key, value);
        persist(editor);
    }

    public static void putStringImmediate(Context context, String key, String value) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit().putString(key, value);
        persistImmediately(editor);
    }

    public static void remove(Context context, String key) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit().remove(key);
        persist(editor);
    }

    public static void removeImmediate(Context context, String key) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit().remove(key);
        persistImmediately(editor);
    }

    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        return getPreferences(context).getBoolean(key, defaultValue);
    }

    public static void putBoolean(Context context, String key, boolean value) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit().putBoolean(key, value);
        persist(editor);
    }
}
