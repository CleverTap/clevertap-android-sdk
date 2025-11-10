package com.clevertap.android.sdk.network

import android.annotation.SuppressLint
import android.content.Context
import com.clevertap.android.sdk.StorageHelper

private const val NAMESPACE_IJ = "IJ"
private const val KEY_I = "comms_i"
private const val KEY_J = "comms_j"

// TODO lp fix shared prefs stuff
internal class IJRepo(private val accountId: String) {

    @SuppressLint("CommitPrefEdits")
    fun setI(context: Context, i: Long) {
        val prefs = StorageHelper.getPreferences(context, NAMESPACE_IJ)
        val editor = prefs.edit()
        editor.putLong("$KEY_I:$accountId", i)
        StorageHelper.persist(editor)
    }

    @SuppressLint("CommitPrefEdits")
    fun setJ(context: Context, j: Long) {
        val prefs = StorageHelper.getPreferences(context, NAMESPACE_IJ)
        val editor = prefs.edit()
        editor.putLong("$KEY_J:$accountId", j)
        StorageHelper.persist(editor)
    }

    fun getI(context: Context): Long {
        return StorageHelper.getLongFromPrefs(
            context,
            accountId,
            KEY_I,
            0,
            NAMESPACE_IJ
        )
    }

    fun getJ(context: Context): Long {
        return StorageHelper.getLongFromPrefs(
            context,
            accountId,
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