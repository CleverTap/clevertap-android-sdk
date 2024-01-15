package com.clevertap.android.sdk.inapp.store.preference

import com.clevertap.android.sdk.store.preference.ICTPreference

/**
 * This is a pref based store backed by a shared-pref file. This file contains only KV pairs for
 * the url assets and their data.
 *
 * Note : do not save anything else in this preference
 */
class InAppAssetsStore(
    private val ctPreference: ICTPreference,
) {

    fun saveAssetUrl(url: String, expiry: Long) {
        ctPreference.writeLong(url, expiry)
    }

    fun clearAssetUrl(url: String) {
        ctPreference.remove(url)
    }

    fun getAllAssetUrls(): Set<String> {
        return ctPreference.readAll()?.keys ?: emptySet()
    }

    fun expiryForUrl(url: String) : Long {
        return ctPreference.readLong(url, 0)
    }
}