package com.clevertap.android.sdk.inapp.store.preference

import com.clevertap.android.sdk.store.preference.ICTPreference

class FileStore(
    private val ctPreference: ICTPreference,
) {

    fun saveFileUrl(url: String, expiry: Long) {
        ctPreference.writeLong(url, expiry)
    }

    fun clearFileUrl(url: String) {
        ctPreference.remove(url)
    }

    fun getAllFileUrls(): Set<String> {
        return ctPreference.readAll()?.keys ?: emptySet()
    }

    fun expiryForUrl(url: String) : Long {
        return ctPreference.readLong(url, 0)
    }
}