package com.clevertap.android.sdk

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread

@Suppress("unused")
internal object StorageHelper {
    @JvmStatic
    fun getPreferences(context: Context, namespace: String?): SharedPreferences {
        var path = Constants.CLEVERTAP_STORAGE_TAG

        if (namespace != null) {
            path += "_$namespace"
        }
        return context.getSharedPreferences(path, Context.MODE_PRIVATE)
    }

    @JvmStatic
    fun getPreferences(context: Context): SharedPreferences {
        return getPreferences(context, null)
    }

    /**
     * returns a String From Local Storage.
     * The key used for getting the value depends upon a few factors:
     * A) When config is default instance:
     * 1. we search for the value for key "[rawKey]:[account_id]" and return it
     * 2. if no value is found for key "[rawKey]:[account_id]", we search for just key "[rawKey]" and return it
     * 3. if no value is found for key "[rawKey]", we return default value
     * B) When config is NOT default instance:
     * 1. we search for the value for key "[rawKey]:[account_id]" and return it
     * 2. if no value is found for key "[rawKey]:[account_id]", we return default value
     */
    @JvmStatic
    fun getStringFromPrefs(
        context: Context,
        accountId: String,
        rawKey: String,
        defaultValue: String?
    ): String? {
        return getString(context, storageKeyWithSuffix(accountId, rawKey), defaultValue)
    }

    @JvmStatic
    fun getString(context: Context, key: String, defaultValue: String?): String? {
        return getPreferences(context).getString(key, defaultValue)
    }

    @JvmStatic
    fun getString(
        context: Context,
        nameSpace: String?,
        key: String,
        defaultValue: String?
    ): String? {
        return getPreferences(context, nameSpace).getString(key, defaultValue)
    }

    @JvmStatic
    fun putString(context: Context, key: String, value: String?) {
        val prefs = getPreferences(context)
        val editor = prefs.edit().putString(key, value)
        persist(editor)
    }

    @JvmStatic
    fun putString(context: Context, accountId: String, key: String, value: String?) {
        val prefKey = storageKeyWithSuffix(accountId, key)
        putString(context, prefKey, value)
    }

    fun putStringImmediate(context: Context, key: String, value: String?) {
        val prefs = getPreferences(context)
        val editor = prefs.edit().putString(key, value)
        persistImmediately(editor)
    }

    @JvmStatic
    fun putStringImmediate(context: Context, accountId: String, key: String, value: String?) {
        putStringImmediate(context, storageKeyWithSuffix(accountId, key), value)
    }

    fun getBoolean(context: Context, key: String, defaultValue: Boolean): Boolean {
        return getPreferences(context).getBoolean(key, defaultValue)
    }

    @JvmStatic
    fun getBooleanFromPrefs(context: Context, accountId: String, rawKey: String): Boolean {
        return getBoolean(context, storageKeyWithSuffix(accountId, rawKey), false)
    }

    @JvmStatic
    fun getInt(context: Context, key: String, defaultValue: Int): Int {
        return getPreferences(context).getInt(key, defaultValue)
    }

    @JvmStatic
    fun getIntFromPrefs(
        context: Context,
        accountId: String,
        rawKey: String,
        defaultValue: Int
    ): Int {
        val key = storageKeyWithSuffix(accountId, rawKey)
        return getInt(context, key, defaultValue)
    }

    fun getLong(context: Context, key: String, defaultValue: Long): Long {
        return getPreferences(context).getLong(key, defaultValue)
    }

    fun getLong(context: Context, nameSpace: String?, key: String, defaultValue: Long): Long {
        return getPreferences(context, nameSpace).getLong(key, defaultValue)
    }

    fun getLongFromPrefs(
        context: Context,
        accountId: String,
        rawKey: String,
        defaultValue: Long,
        nameSpace: String?
    ): Long {
        val key = storageKeyWithSuffix(accountId, rawKey)
        return getLong(context, nameSpace, key, defaultValue)
    }

    @JvmStatic
    fun putBoolean(context: Context, accountId: String, key: String, value: Boolean) {
        putBoolean(context, storageKeyWithSuffix(accountId, key), value)
    }

    fun putBoolean(context: Context, key: String, value: Boolean) {
        val prefs = getPreferences(context)
        val editor = prefs.edit().putBoolean(key, value)
        persist(editor)
    }

    fun putBooleanImmediate(context: Context, key: String, value: Boolean) {
        val prefs = getPreferences(context)
        val editor = prefs.edit().putBoolean(key, value)
        persistImmediately(editor)
    }

    @JvmStatic
    fun putInt(context: Context, key: String, value: Int) {
        val prefs = getPreferences(context)
        val editor = prefs.edit().putInt(key, value)
        persist(editor)
    }

    @JvmStatic
    fun putInt(context: Context, accountId: String, key: String, value: Int) {
        val prefKey = storageKeyWithSuffix(accountId, key)
        putInt(context, prefKey, value)
    }

    fun putIntImmediate(context: Context, key: String, value: Int) {
        val prefs = getPreferences(context)
        val editor = prefs.edit().putInt(key, value)
        persistImmediately(editor)
    }

    fun putLong(context: Context, key: String, value: Long) {
        putLong(context, null, key, value)
    }

    fun putLong(context: Context, namespace: String?, key: String, value: Long) {
        val prefs = getPreferences(context, namespace)
        val editor = prefs.edit().putLong(key, value)
        persist(editor)
    }

    @JvmStatic
    fun remove(context: Context, accountId: String, key: String) {
        remove(context, storageKeyWithSuffix(accountId, key))
    }

    @JvmStatic
    fun remove(context: Context, key: String) {
        val prefs = getPreferences(context)
        val editor = prefs.edit().remove(key)
        persist(editor)
    }

    fun removeImmediate(context: Context, key: String) {
        val prefs = getPreferences(context)
        val editor = prefs.edit().remove(key)
        persistImmediately(editor)
    }

    @JvmStatic
    fun persist(editor: SharedPreferences.Editor) {
        try {
            editor.apply()
        } catch (t: Throwable) {
            Logger.v("CRITICAL: Failed to persist shared preferences!", t)
        }
    }

    /**
     * Use this method, when you are sure that you are on background thread
     */
    @WorkerThread
    fun persistImmediately(editor: SharedPreferences.Editor) {
        try {
            editor.commit()
        } catch (t: Throwable) {
            Logger.v("CRITICAL: Failed to persist shared preferences!", t)
        }
    }

    fun storageKeyWithSuffix(accountID: String, key: String): String {
        return "$key:$accountID"
    }
}
