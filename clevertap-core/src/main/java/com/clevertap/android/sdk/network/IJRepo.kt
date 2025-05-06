package com.clevertap.android.sdk.network

import android.annotation.SuppressLint
import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.StorageHelper

private const val NAMESPACE_IJ = "IJ"
private const val KEY_I = "comms_i"
private const val KEY_J = "comms_j"

internal class IJRepo(private val config: CleverTapInstanceConfig) {

    @SuppressLint("CommitPrefEdits")
    fun setI(context: Context, i: Long) {
        val prefs = StorageHelper.getPreferences(context, NAMESPACE_IJ)
        val editor = prefs.edit()
        editor.putLong(StorageHelper.storageKeyWithSuffix(config.accountId, KEY_I), i)
        StorageHelper.persist(editor)
    }

    @SuppressLint("CommitPrefEdits")
    fun setJ(context: Context, j: Long) {
        val prefs = StorageHelper.getPreferences(context, NAMESPACE_IJ)
        val editor = prefs.edit()
        editor.putLong(StorageHelper.storageKeyWithSuffix(config.accountId, KEY_J), j)
        StorageHelper.persist(editor)
    }

    fun getI(context: Context): Long {
        return StorageHelper.getLongFromPrefs(
            context,
            config,
            KEY_I,
            0,
            NAMESPACE_IJ
        )
    }

    fun getJ(context: Context): Long {
        return StorageHelper.getLongFromPrefs(
            context,
            config,
            KEY_J,
            0,
            NAMESPACE_IJ
        )
    }

    fun clearIJ(context: Context) {
        val editor = StorageHelper.getPreferences(context, NAMESPACE_IJ).edit()
        editor.clear()
        StorageHelper.persist(editor)
    }
}