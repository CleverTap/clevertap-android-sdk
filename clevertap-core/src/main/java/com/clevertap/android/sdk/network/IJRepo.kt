package com.clevertap.android.sdk.network

import android.annotation.SuppressLint
import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.StorageHelper
import androidx.core.content.edit

private const val NAMESPACE_IJ = "IJ"
private const val KEY_I = "comms_i"
private const val KEY_J = "comms_j"

internal class IJRepo(private val config: CleverTapInstanceConfig) {

    @SuppressLint("CommitPrefEdits")
    fun setI(context: Context, i: Long) {
        StorageHelper.putLong(
            context = context,
            namespace = NAMESPACE_IJ,
            key = "$KEY_I:${config.accountId}",
            value = i
        )
    }

    @SuppressLint("CommitPrefEdits")
    fun setJ(context: Context, j: Long) {
        StorageHelper.putLong(
            context = context,
            namespace = NAMESPACE_IJ,
            key = "$KEY_J:${config.accountId}",
            value = j
        )
    }

    fun getI(context: Context): Long {
        return StorageHelper.getLongFromPrefs(
            context,
            config.accountId,
            KEY_I,
            0,
            NAMESPACE_IJ
        )
    }

    fun getJ(context: Context): Long {
        return StorageHelper.getLongFromPrefs(
            context,
            config.accountId,
            KEY_J,
            0,
            NAMESPACE_IJ
        )
    }

    fun clearIJ(context: Context) {
        StorageHelper.getPreferences(context, NAMESPACE_IJ).edit { clear() }
    }
}