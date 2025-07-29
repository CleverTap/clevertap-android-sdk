package com.clevertap.android.sdk;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;

@SuppressWarnings("unused")
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class StorageHelper {

    public static SharedPreferences getPreferences(@NonNull Context context, String namespace) {
        String path = Constants.CLEVERTAP_STORAGE_TAG;

        if (namespace != null) {
            path += "_" + namespace;
        }
        return context.getSharedPreferences(path, Context.MODE_PRIVATE);
    }

    public static SharedPreferences getPreferences(@NonNull  Context context) {
        return getPreferences(context, null);
    }

    /**
     * returns a String From Local Storage.
     * The key used for getting the value depends upon a few factors:
     * A) When config is default instance:
     *      1. we search for the value for key "[rawKey]:[account_id]" and return it
     *      2. if no value is found for key "[rawKey]:[account_id]", we search for just key "[rawKey]" and return it
     *      3. if no value is found for key "[rawKey]", we return default value
     * B) When config is NOT default instance:
     *      1. we search for the value for key "[rawKey]:[account_id]" and return it
     *      2. if no value is found for key "[rawKey]:[account_id]", we return default value
     */
    public static String getStringFromPrefs(@NonNull Context context, @NonNull String accountId, String rawKey, String defaultValue) {
        return getString(context, storageKeyWithSuffix(accountId, rawKey), defaultValue);
    }

    public static String getString(@NonNull Context context, @NonNull String key, String defaultValue) {
        return getPreferences(context).getString(key, defaultValue);
    }

    static String getString(Context context, String nameSpace, String key, String defaultValue) {
        return getPreferences(context, nameSpace).getString(key, defaultValue);
    }

    public static void putString(Context context, String key, String value) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit().putString(key, value);
        persist(editor);
    }

    public static void putString(Context context, String accountId, String key, String value) {
        String prefKey = storageKeyWithSuffix(accountId, key);
        putString(context, prefKey, value);
    }

    public static void putStringImmediate(Context context, String key, String value) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit().putString(key, value);
        persistImmediately(editor);
    }

    public static void putStringImmediate(Context context, String accountId, String key, String value) {
        putStringImmediate(context, storageKeyWithSuffix(accountId, key), value);
    }

    @SuppressWarnings("SameParameterValue")
    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        return getPreferences(context).getBoolean(key, defaultValue);
    }

    public static boolean getBooleanFromPrefs(Context context, String accountId, String rawKey) {
        return getBoolean(context, storageKeyWithSuffix(accountId, rawKey), false);
    }

    public static int getInt(Context context, String key, int defaultValue) {
        return getPreferences(context).getInt(key, defaultValue);
    }

    @SuppressWarnings("SameParameterValue")
    public static int getIntFromPrefs(Context context, String accountId, String rawKey, int defaultValue) {
        String key = storageKeyWithSuffix(accountId, rawKey);
        return getInt(context, key, defaultValue);
    }

    static long getLong(Context context, String key, long defaultValue) {
        return getPreferences(context).getLong(key, defaultValue);
    }

    static long getLong(Context context, String nameSpace, String key, long defaultValue) {
        return getPreferences(context, nameSpace).getLong(key, defaultValue);
    }

    @SuppressWarnings("SameParameterValue")
    public static long getLongFromPrefs(
            Context context,
            String accountId,
            String rawKey,
            int defaultValue,
            String nameSpace
    ) {
        String key = storageKeyWithSuffix(accountId, rawKey);
        return getLong(context, nameSpace, key, defaultValue);
    }

    public static void putBoolean(Context context, String accountId, String key, boolean value) {
        putBoolean(context, storageKeyWithSuffix(accountId, key), value);
    }

    public static void putBoolean(Context context, String key, boolean value) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit().putBoolean(key, value);
        persist(editor);
    }

    public static void putBooleanImmediate(Context context, String key, boolean value) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit().putBoolean(key, value);
        persistImmediately(editor);
    }

    public static void putInt(Context context, String key, int value) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit().putInt(key, value);
        persist(editor);
    }

    public static void putInt(Context context, String accountId, String key, int value) {
        String prefKey = storageKeyWithSuffix(accountId, key);
        putInt(context, prefKey, value);
    }

    public static void putIntImmediate(Context context, String key, int value) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit().putInt(key, value);
        persistImmediately(editor);
    }

    static void putLong(Context context, String key, long value) {
        SharedPreferences prefs = getPreferences(context);
        SharedPreferences.Editor editor = prefs.edit().putLong(key, value);
        persist(editor);
    }

    public static void remove(Context context, String accountId, String key) {
        remove(context, storageKeyWithSuffix(accountId, key));
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

    public static void persist(final SharedPreferences.Editor editor) {
        try {
            editor.apply();
        } catch (Throwable t) {
            Logger.v("CRITICAL: Failed to persist shared preferences!", t);
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
            Logger.v("CRITICAL: Failed to persist shared preferences!", t);
        }
    }

    private static String storageKeyWithSuffix(String accountID, @NonNull String key) {
        return key + ":" + accountID;
    }
}
