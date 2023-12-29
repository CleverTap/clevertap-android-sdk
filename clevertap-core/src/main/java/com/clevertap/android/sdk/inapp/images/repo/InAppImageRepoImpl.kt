package com.clevertap.android.sdk.inapp.images.repo

import com.clevertap.android.sdk.inapp.images.cleanup.InAppCleanupStrategy
import com.clevertap.android.sdk.inapp.images.preload.InAppImagePreloaderStrategy
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore
import com.clevertap.android.sdk.inapp.store.preference.LegacyInAppStore

internal class InAppImageRepoImpl(
    override val cleanupStrategy: InAppCleanupStrategy,
    override val preloaderStrategy: InAppImagePreloaderStrategy,
    private val inAppAssetsStore: InAppAssetsStore,
    private val legacyInAppsStore: LegacyInAppStore
) : InAppResourcesRepo {

    companion object {
        const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000
        private const val DAYS_FOR_EXPIRY = 14

        // 14 days
        const val EXPIRY_OFFSET_MILLIS = DAY_IN_MILLIS * DAYS_FOR_EXPIRY
    }

    /**
     * Fetches all images in parallel and registers successful url in repo
     */
    override fun fetchAllImages(urls: List<String>) {

        val successBlock: (url: String) -> Unit = { url ->
            val expiry = System.currentTimeMillis() + EXPIRY_OFFSET_MILLIS
            inAppAssetsStore.saveAssetUrl(url = url, expiry = expiry)
        }

        preloaderStrategy.preloadImages(urls, successBlock)
    }

    /**
     * Checks all existing cached data and check if it is in valid urls, if not evict item from cache
     */
    override fun cleanupStaleImages(validUrls: List<String>) {

        val currentTime = System.currentTimeMillis()

        if (currentTime - legacyInAppsStore.lastCleanupTs() < EXPIRY_OFFSET_MILLIS) {
            // limiting cleanup once per 14 days
            return
        }

        val valid = validUrls.associateWith { it }

        val allAssetUrls = inAppAssetsStore.getAllAssetUrls()

        val cleanupUrls = allAssetUrls
            .toMutableSet()
            .filter { key ->
                valid.contains(key).not()
                        && (currentTime > inAppAssetsStore.expiryForUrl(key))
            }

        val successBlock: (url: String) -> Unit = { url ->
            inAppAssetsStore.clearAssetUrl(url)
        }

        cleanupStrategy.clearAssets(cleanupUrls, successBlock)
        legacyInAppsStore.updateAssetCleanupTs(currentTime)
    }
}