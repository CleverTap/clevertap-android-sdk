package com.clevertap.android.sdk.store.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import androidx.annotation.WorkerThread
import java.lang.ref.WeakReference

class CTPreference(context: Context, private var prefName: String? = null) : ICTPreference {

    private val contextRef = WeakReference(context)

    override fun readString(key: String, default: String): String? {
        val prefs = sharedPrefs() ?: return default
        return prefs.getString(key, default)
    }

    override fun readBoolean(key: String, default: Boolean): Boolean {
        val prefs = sharedPrefs() ?: return default
        return prefs.getBoolean(key, default)
    }

    override fun readInt(key: String, default: Int): Int {
        val prefs = sharedPrefs() ?: return default
        return prefs.getInt(key, default)
    }

    override fun readLong(key: String, default: Long): Long {
        val prefs = sharedPrefs() ?: return default
        return prefs.getLong(key, default)
    }

    override fun readFloat(key: String, default: Float): Float {
        val prefs = sharedPrefs() ?: return default
        return prefs.getFloat(key, default)
    }

    override fun readStringSet(key: String, default: Set<String>): Set<String>? {
        val prefs = sharedPrefs() ?: return default
        return prefs.getStringSet(key, default)
    }

    override fun readAll(): Map<String, *>? {
        val prefs = sharedPrefs() ?: return emptyMap<String, Any>()
        return prefs.all
    }

    override fun writeString(key: String, value: String) {
        val prefs = sharedPrefs() ?: return
        prefs.edit().putString(key, value).apply()
    }

    @WorkerThread
    @SuppressLint("ApplySharedPref")
    override fun writeStringImmediate(key: String, value: String) {
        val prefs = sharedPrefs() ?: return
        prefs.edit().putString(key, value).commit()
    }

    override fun writeBoolean(key: String, value: Boolean) {
        val prefs = sharedPrefs() ?: return
        prefs.edit().putBoolean(key, value).apply()
    }

    @WorkerThread
    @SuppressLint("ApplySharedPref")
    override fun writeBooleanImmediate(key: String, value: Boolean) {
        val prefs = sharedPrefs() ?: return
        prefs.edit().putBoolean(key, value).commit()
    }

    override fun writeInt(key: String, value: Int) {
        val prefs = sharedPrefs() ?: return
        prefs.edit().putInt(key, value).apply()
    }

    @WorkerThread
    @SuppressLint("ApplySharedPref")
    override fun writeIntImmediate(key: String, value: Int) {
        val prefs = sharedPrefs() ?: return
        prefs.edit().putInt(key, value).commit()
    }

    override fun writeLong(key: String, value: Long) {
        val prefs = sharedPrefs() ?: return
        prefs.edit().putLong(key, value).apply()
    }

    @WorkerThread
    @SuppressLint("ApplySharedPref")
    override fun writeLongImmediate(key: String, value: Long) {
        val prefs = sharedPrefs() ?: return
        prefs.edit().putLong(key, value).commit()
    }

    override fun writeFloat(key: String, value: Float) {
        val prefs = sharedPrefs() ?: return
        prefs.edit().putFloat(key, value).apply()
    }

    @WorkerThread
    @SuppressLint("ApplySharedPref")
    override fun writeFloatImmediate(key: String, value: Float) {
        val prefs = sharedPrefs() ?: return
        prefs.edit().putFloat(key, value).commit()
    }

    override fun writeStringSet(key: String, value: Set<String>) {
        val prefs = sharedPrefs() ?: return
        val editor = prefs.edit()
        editor.putStringSet(key, value).apply()
    }

    @WorkerThread
    @SuppressLint("ApplySharedPref")
    override fun writeStringSetImmediate(key: String, value: Set<String>) {
        val prefs = sharedPrefs() ?: return
        val editor = prefs.edit()
        editor.putStringSet(key, value).commit()
    }

    override fun writeMap(key: String, value: Map<String, *>) {
        val prefs = sharedPrefs() ?: return
        writeMapToEditor(prefs, value).apply()
    }

    @WorkerThread
    @SuppressLint("ApplySharedPref")
    override fun writeMapImmediate(key: String, value: Map<String, *>) {
        val prefs = sharedPrefs() ?: return
        writeMapToEditor(prefs, value).commit()
    }

    @SuppressLint("CommitPrefEdits")
    private fun writeMapToEditor(
        prefs: SharedPreferences,
        value: Map<String, *>,
    ): Editor {
        val editor = prefs.edit()
        for ((subKey, subValue) in value) {
            when (subValue) {
                is String -> editor.putString(subKey, subValue)
                is Boolean -> editor.putBoolean(subKey, subValue)
                is Int -> editor.putInt(subKey, subValue)
                is Long -> editor.putLong(subKey, subValue)
                is Float -> editor.putFloat(subKey, subValue)
            }
        }
        return editor
    }

    override fun isEmpty(): Boolean {
        val prefs = sharedPrefs() ?: return true
        return prefs.all.isEmpty()
    }

    override fun size(): Int {
        val prefs = sharedPrefs() ?: return 0
        return prefs.all.size
    }

    override fun remove(key: String) {
        val prefs = sharedPrefs() ?: return
        prefs.edit().remove(key).apply()
    }

    @WorkerThread
    @SuppressLint("ApplySharedPref")
    override fun removeImmediate(key: String) {
        val prefs = sharedPrefs() ?: return
        prefs.edit().remove(key).commit()
    }

    override fun changePreferenceName(prefName: String) {
        this.prefName = prefName
    }

    private fun sharedPrefs(): SharedPreferences? {
        val context = contextRef.get() ?: return null
        return context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
    }
}