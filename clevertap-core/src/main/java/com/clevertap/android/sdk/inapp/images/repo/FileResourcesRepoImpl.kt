package com.clevertap.android.sdk.inapp.images.repo

import androidx.annotation.WorkerThread
import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.images.cleanup.FileCleanupStrategy
import com.clevertap.android.sdk.inapp.images.preload.FilePreloaderStrategy
import com.clevertap.android.sdk.inapp.store.preference.FileStore
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore
import com.clevertap.android.sdk.inapp.store.preference.LegacyInAppStore
import kotlin.math.max

internal class FileResourcesRepoImpl constructor(
    override val cleanupStrategy: FileCleanupStrategy,
    override val preloaderStrategy: FilePreloaderStrategy,
    private val inAppAssetsStore: InAppAssetsStore,
    private val fileStore: FileStore,
    private val legacyInAppsStore: LegacyInAppStore
) : FileResourcesRepo {

    companion object {

        const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000
        private const val DAYS_FOR_EXPIRY = 14

        // 14 days
        const val EXPIRY_OFFSET_MILLIS = DAY_IN_MILLIS * DAYS_FOR_EXPIRY

        private val listeners = setOf<(urls: List<String>) -> Map<String, Boolean>>()
        private val listenerss = mutableSetOf<DownloadListener>()

        private val downloadInProgressUrls = object : HashMap<String, DownloadState>() {
            override fun put(key: String, value: DownloadState): DownloadState? {

                listenerss.forEach { listener ->
                    if (listener.url == key) {
                        when (value) {
                            DownloadState.QUEUED,
                            DownloadState.IN_PROGRESS -> {
                                //noop
                            }
                            DownloadState.SUCCESSFUL -> {
                                listener.callback.invoke(listener.url to true)
                            }
                            DownloadState.FAILED -> {
                                listener.callback.invoke(listener.url to false)
                            }
                        }
                    }
                }

                return super.put(key, value)
            }
        }
        private val fetchAllFilesLock = Any()
    }

    @WorkerThread
    override fun preloadFilesAndCache(
        urlMeta: List<Pair<String, CtCacheType>>,
        completionCallback: (urlStatusMap: Map<String, Boolean>) -> Unit
    ) {

        val successBlock: (urlMeta: Pair<String, CtCacheType>) -> Unit = { meta ->
            val url = meta.first
            val expiry = System.currentTimeMillis() + EXPIRY_OFFSET_MILLIS

            when (meta.second) {
                CtCacheType.IMAGE,
                CtCacheType.GIF -> {
                    inAppAssetsStore.saveAssetUrl(url = url, expiry = expiry)
                    fileStore.saveFileUrl(url = url, expiry = expiry)
                }
                CtCacheType.FILES -> {
                    fileStore.saveFileUrl(url = url, expiry = expiry)
                }
            }
            synchronized(fetchAllFilesLock) {
                downloadInProgressUrls.put(meta.first, DownloadState.SUCCESSFUL)
            }
        }
        val failureBlock: (urlMeta: Pair<String, CtCacheType>) -> Unit = { meta ->
            synchronized(fetchAllFilesLock) {
                downloadInProgressUrls.put(meta.first, DownloadState.FAILED)
            }
        }

        val started : (urlMeta: Pair<String, CtCacheType>) -> Unit = { meta ->
            synchronized(fetchAllFilesLock) {
                downloadInProgressUrls.put(meta.first, DownloadState.IN_PROGRESS)
            }
        }

        preloaderStrategy.preloadFilesAndCache(
            urlMetas = urlMeta,
            successBlock = successBlock,
            failureBlock = failureBlock,
            startedBlock = started,
            preloadFinished = completionCallback
        )
    }

    override fun cleanupStaleFiles(
        urls: List<String>
    ) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - legacyInAppsStore.lastCleanupTs() < EXPIRY_OFFSET_MILLIS) {
            // limiting cleanup once per 14 days
            return
        }

        cleanupStaleFilesNow(
            validUrls = urls,
            currentTime = currentTime
        )
        legacyInAppsStore.updateAssetCleanupTs(currentTime)
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

enum class DownloadState {
    QUEUED, IN_PROGRESS, SUCCESSFUL, FAILED
}

data class DownloadListener( // todo check if needed
    val url: String,
    val callback: (Pair<String, Boolean>) -> Unit
)