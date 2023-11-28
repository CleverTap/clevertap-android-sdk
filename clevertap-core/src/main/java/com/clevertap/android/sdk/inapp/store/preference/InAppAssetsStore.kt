package com.clevertap.android.sdk.inapp.store.preference

import com.clevertap.android.sdk.store.preference.ICTPreference

class InAppAssetsStore(
    private val ctPreference: ICTPreference,
) {

    companion object {
        private const val INAPPS_ASSETS = "inapps_assets"
    }

    fun saveAllAssetUrls(urls: Set<String>) {
        ctPreference.writeStringSet(INAPPS_ASSETS, urls)
    }

    fun saveAssetUrl(url: String) {
        val set = ctPreference.readStringSet(INAPPS_ASSETS, emptySet()) ?: emptySet()
        val updated = mutableSetOf<String>().apply {
            add(url)
            addAll(set)
        }

        ctPreference.writeStringSet(INAPPS_ASSETS, updated)
    }

    fun getAllAssetUrls(): Set<String> {
        return ctPreference.readStringSet(INAPPS_ASSETS, emptySet()) ?: emptySet()
    }
}