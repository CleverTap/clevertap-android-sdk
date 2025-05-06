package com.clevertap.android.sdk.network

import android.annotation.SuppressLint
import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.StorageHelper

internal class IJRepo(private val config: CleverTapInstanceConfig) {

    @SuppressLint("CommitPrefEdits")
    fun setI(context: Context, i: Long) {
        val prefs = StorageHelper.getPreferences(context, Constants.NAMESPACE_IJ)
        val editor = prefs.edit()
        editor.putLong(StorageHelper.storageKeyWithSuffix(config.accountId, Constants.KEY_I), i)
        StorageHelper.persist(editor)
    }

    @SuppressLint("CommitPrefEdits")
    fun setJ(context: Context, j: Long) {
        val prefs = StorageHelper.getPreferences(context, Constants.NAMESPACE_IJ)
        val editor = prefs.edit()
        editor.putLong(StorageHelper.storageKeyWithSuffix(config.accountId, Constants.KEY_J), j)
        StorageHelper.persist(editor)
    }

    fun getI(context: Context): Long {
        return StorageHelper.getLongFromPrefs(
            context,
            config,
            Constants.KEY_I,
            0,
            Constants.NAMESPACE_IJ
        )
    }

    fun getJ(context: Context): Long {
        return StorageHelper.getLongFromPrefs(
            context,
            config,
            Constants.KEY_J,
            0,
            Constants.NAMESPACE_IJ
        )
    }
}