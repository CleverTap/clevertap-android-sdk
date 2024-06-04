package com.clevertap.android.sdk.inapp.images.repo

import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.inapp.images.cleanup.FileCleanupStrategy
import com.clevertap.android.sdk.inapp.images.preload.FilePreloaderStrategy
import com.clevertap.android.sdk.inapp.store.preference.FileStore
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore
import com.clevertap.android.sdk.inapp.store.preference.LegacyInAppStore
import java.util.concurrent.ConcurrentHashMap

internal class FileResourcesRepoImpl(
    override val cleanupStrategy: FileCleanupStrategy,
    override val preloaderStrategy: FilePreloaderStrategy,
    private val inAppAssetsStore: InAppAssetsStore,
    private val fileStore: FileStore,
    private val legacyInAppsStore: LegacyInAppStore
) : FileResourcesRepo {

    private val downloadInProgressUrls = ConcurrentHashMap<String, MutableList<(String, Boolean) -> Unit>>()
    private val lock = Any()

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
            inAppAssetsStore.saveAssetUrl(url = url, expiry = expiry)// uncommon
        }

        preloaderStrategy.preloadImages(urls, successBlock)
    }

    override fun fetchAllGifs(urls: List<String>) {
        val successBlock: (url: String) -> Unit = { url ->
            val expiry = System.currentTimeMillis() + EXPIRY_OFFSET_MILLIS
            inAppAssetsStore.saveAssetUrl(url = url, expiry = expiry)
        }

        preloaderStrategy.preloadGifs(urls, successBlock)
    }

    @WorkerThread
    override fun fetchAllFiles(
        urls: List<String>,
        completionCallback: (status: Boolean, urlStatusMap: Map<String, Boolean>) -> Unit
    ) {
        val urlStatusMap = ConcurrentHashMap<String, Boolean>()

        val newCompletionCallback: (String, Boolean) -> Unit = { url, status ->
            urlStatusMap[url] = status
            checkCompletion(urls.size, urlStatusMap, completionCallback)
        }
        val successBlock: (url: String) -> Unit = { url ->
            synchronized(lock) {
            val expiry = System.currentTimeMillis() + EXPIRY_OFFSET_MILLIS
            fileStore.saveFileUrl(url = url, expiry = expiry)
            val callbacks = downloadInProgressUrls.remove(url)
            callbacks?.forEach { it(url, true) }
            }
        }
        val failureBlock: (String) -> Unit = { url ->
            synchronized(lock) {
                val callbacks = downloadInProgressUrls.remove(url)
                callbacks?.forEach { it(url, false) }
            }
        }

        synchronized(lock) {
            if (downloadInProgressUrls.isEmpty()) {
                urls.forEach { url ->
                    downloadInProgressUrls[url] = mutableListOf(newCompletionCallback)
                }
                preloaderStrategy.preloadFiles(urls, successBlock, failureBlock)
            } else {
                val filteredUrls = mutableListOf<String>()
                urls.forEach { url ->
                    if (downloadInProgressUrls.containsKey(url)) {
                        downloadInProgressUrls[url]?.add(newCompletionCallback)
                    } else {
                        downloadInProgressUrls[url] = mutableListOf(newCompletionCallback)
                        filteredUrls.add(url)
                    }
                }
                preloaderStrategy.preloadFiles(filteredUrls, successBlock, failureBlock)
            }
        }
    }

    private fun checkCompletion(
        totalUrls: Int,
        urlStatusMap: ConcurrentHashMap<String, Boolean>,
        completionCallback: (status: Boolean, urlStatusMap: Map<String, Boolean>) -> Unit
    ) {
        if (urlStatusMap.size == totalUrls) {
            val allSuccessful = urlStatusMap.values.all { it }
            completionCallback(allSuccessful, urlStatusMap)
        }
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

        cleanupStaleImagesNow(validUrls, currentTime)
        legacyInAppsStore.updateAssetCleanupTs(currentTime)
    }

    override fun cleanupStaleFiles(validUrls: List<String>) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - legacyInAppsStore.lastCleanupTsForFiles() < EXPIRY_OFFSET_MILLIS) {
            // limiting cleanup once per 14 days
            return
        }

        cleanupStaleFilesNow(validUrls, currentTime)
        legacyInAppsStore.updateFileCleanupTs(currentTime)
    }

    @JvmOverloads
    fun cleanupStaleImagesNow(
        validUrls: List<String> = emptyList(),
        currentTime: Long = System.currentTimeMillis()
    ) {
        val valid = validUrls.associateWith { it }

        val allAssetUrls = inAppAssetsStore.getAllAssetUrls()

        val cleanupUrls = allAssetUrls
            .toMutableSet()
            .filter { key ->
                valid.contains(key).not()
                        && (currentTime > inAppAssetsStore.expiryForUrl(key))
            }

        cleanupAllImages(cleanupUrls)
    }

    @JvmOverloads
    fun cleanupStaleFilesNow(
        validUrls: List<String> = emptyList(),
        currentTime: Long = System.currentTimeMillis()
    ) {
        val valid = validUrls.associateWith { it }

        val allFileUrls = fileStore.getAllFileUrls()

        val cleanupFileUrls = allFileUrls
            .toMutableSet()
            .filter { key ->
                valid.contains(key).not()
                        && (currentTime > fileStore.expiryForUrl(key))
            }

        cleanupAllFiles(cleanupFileUrls)
    }

    @JvmOverloads
    fun cleanupAllImages(
        cleanupUrls: List<String> = inAppAssetsStore.getAllAssetUrls().toList()
    ) {
        val successBlock: (url: String) -> Unit = { url ->
            inAppAssetsStore.clearAssetUrl(url)
        }

        cleanupStrategy.clearInAppAssets(cleanupUrls, successBlock)
    }

    @JvmOverloads
    fun cleanupAllFiles(
        cleanupUrls: List<String> = fileStore.getAllFileUrls().toList()
    ) {
        val successBlock: (url: String) -> Unit = { url ->
            fileStore.clearFileUrl(url)
        }

        cleanupStrategy.clearFileAssets(cleanupUrls, successBlock)
    }
}