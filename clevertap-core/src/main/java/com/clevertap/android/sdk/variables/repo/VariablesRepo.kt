package com.clevertap.android.sdk.variables.repo

import android.content.Context
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.StorageHelper.getStringFromPrefs
import com.clevertap.android.sdk.StorageHelper.putString
import com.clevertap.android.sdk.db.DBEncryptionHandler

internal class VariablesRepo(
    val context: Context,
    val accountId: String,
    val dbEncryptionHandler: DBEncryptionHandler
) {

    fun storeDataInCache(data: String) {
        Logger.d("storeDataInCache() called with: data = [$data]")
        try {
            val encryptedData: String = dbEncryptionHandler.wrapDbData(data)
            putString(
                context,
                accountId,
                Constants.CACHED_VARIABLES_KEY,
                encryptedData
            )
        } catch (t: Throwable) {
            Logger.d("storeDataInCache failed", t)
        }
    }

    fun loadDataFromCache(): String? {
        var cache = getStringFromPrefs(
            context,
            accountId,
            Constants.CACHED_VARIABLES_KEY,
            "{}"
        )
        try {
            cache = dbEncryptionHandler.unwrapDbData(cache!!)
        } catch (t: Throwable) {
            Logger.d("loadDataFromCache failed in decryption step", t)
        }
        Logger.d("VarCache loaded cache data:\n$cache")
        return cache
    }
}