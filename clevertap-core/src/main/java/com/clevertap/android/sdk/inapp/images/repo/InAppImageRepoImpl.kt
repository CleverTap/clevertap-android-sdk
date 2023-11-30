package com.clevertap.android.sdk.inapp.images.repo

import com.clevertap.android.sdk.inapp.images.cleanup.InAppCleanupStrategy
import com.clevertap.android.sdk.inapp.images.preload.InAppImagePreloaderStrategy
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore

internal class InAppImageRepoImpl(
    override val cleanupStrategy: InAppCleanupStrategy,
    override val preloaderStrategy: InAppImagePreloaderStrategy,
    private val inAppAssetsStore: InAppAssetsStore
) : InAppResourcesRepo {

    companion object {
        private const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000
        private const val DAYS_FOR_EXPIRY = 14

        // 14 days
        private const val EXPIRY_OFFSET_MILLIS = DAY_IN_MILLIS * DAYS_FOR_EXPIRY
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
    }
}