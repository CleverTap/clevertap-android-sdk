package com.clevertap.android.sdk.inapp.images.repo

import com.clevertap.android.sdk.inapp.images.cleanup.InAppCleanupStrategy
import com.clevertap.android.sdk.inapp.images.preload.InAppImagePreloaderStrategy
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore

internal class InAppImageRepoImpl(
    override val cleanupStrategy: InAppCleanupStrategy,
    override val preloaderStrategy: InAppImagePreloaderStrategy,
    private val inAppAssetsStore: InAppAssetsStore
) : InAppResourcesRepo {

    /**
     * Fetches all images in parallel and registers successful url in repo
     */
    override fun fetchAllImages(urls: List<String>) {

        val successBlock: (url: String) -> Unit = { url ->
            inAppAssetsStore.saveAssetUrl(url)
        }

        preloaderStrategy.preloadImages(urls, successBlock)
    }

    /**
     * Checks all existing cached data and check if it is in valid urls, if not evict item from cache
     */
    override fun cleanupStaleImages(validUrls: List<String>) {
        // saved list
        // valid list
        // list to be deleted can be formed

        val valid = validUrls.associateWith { it }

        val allAssetUrls = inAppAssetsStore.getAllAssetUrls()

        val cleanupUrls = allAssetUrls.toMutableSet().filter { key ->
            valid.contains(key).not()
        }

        cleanupStrategy.clearAssets(cleanupUrls)
    }
}