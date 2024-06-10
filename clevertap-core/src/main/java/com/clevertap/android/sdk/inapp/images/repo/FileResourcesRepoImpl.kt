package com.clevertap.android.sdk.inapp.images.repo

import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.images.cleanup.FileCleanupStrategy
import com.clevertap.android.sdk.inapp.images.preload.FilePreloaderStrategy
import com.clevertap.android.sdk.inapp.store.preference.FileStore
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore
import com.clevertap.android.sdk.inapp.store.preference.LegacyInAppStore
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

internal class FileResourcesRepoImpl constructor(
    override val cleanupStrategy: FileCleanupStrategy,
    override val preloaderStrategy: FilePreloaderStrategy,
    private val inAppAssetsStore: InAppAssetsStore,
    private val fileStore: FileStore,
    private val legacyInAppsStore: LegacyInAppStore
) : FileResourcesRepo {

    //if we want to call fetchAll() API using multiple instances of this class then downloadInProgressUrls must be static
    private val downloadInProgressUrls = ConcurrentHashMap<String, MutableList<(String, Boolean) -> Unit>>()
    private val fetchAllFilesLock = Any()

    companion object {

        const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000
        private const val DAYS_FOR_EXPIRY = 14

        // 14 days
        const val EXPIRY_OFFSET_MILLIS = DAY_IN_MILLIS * DAYS_FOR_EXPIRY
    }

    /*@WorkerThread
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
            synchronized(fetchAllFilesLock) {
                val expiry = System.currentTimeMillis() + EXPIRY_OFFSET_MILLIS
                fileStore.saveFileUrl(url = url, expiry = expiry)
                val callbacks = downloadInProgressUrls.remove(url)
                callbacks?.forEach { it(url, true) }
            }
        }
        val failureBlock: (String) -> Unit = { url ->
            synchronized(fetchAllFilesLock) {
                val callbacks = downloadInProgressUrls.remove(url)
                callbacks?.forEach { it(url, false) }
            }
        }

        synchronized(fetchAllFilesLock) {
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
    }*/

    override fun preloadFilesAndCache(
        urlMeta: List<Pair<String, CtCacheType>>,
    ) {
        val successBlock: (urlMeta: Pair<String, CtCacheType>) -> Unit = { meta ->
            val expiry = System.currentTimeMillis() + EXPIRY_OFFSET_MILLIS

            when (meta.second) {
                CtCacheType.IMAGE,
                CtCacheType.GIF -> {
                    inAppAssetsStore.saveAssetUrl(url = meta.first, expiry = expiry)
                    fileStore.saveFileUrl(url = meta.first, expiry = expiry)
                }
                CtCacheType.FILES -> {
                    fileStore.saveFileUrl(url = meta.first, expiry = expiry)
                }
            }

        }
        preloaderStrategy.preloadFilesAndCache(
            urlMetas = urlMeta,
            successBlock = successBlock
        )
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
    override fun cleanupStaleFiles(
        urls: List<String>
    ) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - legacyInAppsStore.lastCleanupTsForFiles() < EXPIRY_OFFSET_MILLIS) {
            // limiting cleanup once per 14 days
            return
        }

        cleanupStaleFilesNow(
            validUrls = urls,
            currentTime = currentTime
        )
        legacyInAppsStore.updateFileCleanupTs(currentTime)
    }

    override fun cleanupExpiredInAppsResources() {
        cleanupStaleFilesNow(
            allFileUrls = inAppAssetsStore.getAllAssetUrls(),
            expiryTs = { key ->
                inAppAssetsStore.expiryForUrl(key)
            }
        )
    }

    override fun cleanupInAppsResources() {
        cleanupStaleFilesNow(
            allFileUrls = inAppAssetsStore.getAllAssetUrls(),
            expiryTs = { key ->
                inAppAssetsStore.expiryForUrl(key)
            }
        )
    }

    private fun cleanupStaleFilesNow(
        validUrls: List<String> = emptyList(),
        currentTime: Long = System.currentTimeMillis(),
        allFileUrls: Set<String> = fileStore.getAllFileUrls() + inAppAssetsStore.getAllAssetUrls(),
        expiryTs: (url: String) -> Long = { key ->
            max(fileStore.expiryForUrl(key), inAppAssetsStore.expiryForUrl(key))
        }
    ) {
        val valid = validUrls.associateWith { it }

        val cleanupFileUrls = allFileUrls
            .toMutableSet()
            .filter { key ->

                // check if url is still valid, if so then dont clear
                val first = valid.contains(key).not()

                // check current time is greater than expiry for url
                val second = currentTime > expiryTs(key)

                first && second
            }

        cleanupAllFiles(cleanupFileUrls)
    }

    private fun cleanupAllFiles(
        cleanupUrls: List<String>
    ) {
        val successBlock: (url: String) -> Unit = { url ->
            fileStore.clearFileUrl(url)
            inAppAssetsStore.clearAssetUrl(url)
        }

        cleanupStrategy.clearFileAssets(cleanupUrls, successBlock)
    }
}