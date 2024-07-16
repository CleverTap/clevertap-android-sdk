package com.clevertap.android.sdk.inapp.images.repo

import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.images.cleanup.FileCleanupStrategy
import com.clevertap.android.sdk.inapp.images.preload.FilePreloaderStrategy
import com.clevertap.android.sdk.inapp.store.preference.FileStore
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore
import com.clevertap.android.sdk.inapp.store.preference.LegacyInAppStore
import kotlin.math.max
import kotlin.time.Duration.Companion.days

internal const val TAG_FILE_DOWNLOAD = "FileDownload"
/**
 * Implementation of [FileResourcesRepo] that manages the preloading, caching, and cleanup of file resources.
 *
 * @param cleanupStrategy The strategy for cleaning up stale or unnecessary files.
 * @param preloaderStrategy The strategy for preloading files to improve performance.
 * @param inAppAssetsStore The store for managing in-app asset URLs and their expiry times.
 * @param fileStore The store for managing file URLs and their expiry times.
 * @param legacyInAppsStore The store for managing legacy in-app data.
 */
internal class FileResourcesRepoImpl constructor(
    override val cleanupStrategy: FileCleanupStrategy,
    override val preloaderStrategy: FilePreloaderStrategy,
    private val inAppAssetsStore: InAppAssetsStore,
    private val fileStore: FileStore,
    private val legacyInAppsStore: LegacyInAppStore
) : FileResourcesRepo {

    companion object {

        // 14 days
        private val EXPIRY_OFFSET_MILLIS = 14.days.inWholeMilliseconds

        private val urlTriggers = mutableSetOf<DownloadTriggerForUrls>()

        private val downloadInProgressUrls = HashMap<String, DownloadState>()
        private val fetchAllFilesLock = Any()
        /**
         * Saves the expiry time for a downloaded URL in the appropriate store.
         *
         * @param urlMeta The pair of URL and its [CtCacheType].
         * @param storePair The pair of [FileStore] and [InAppAssetsStore] to save the expiry time.
         */
        @JvmStatic
        fun saveUrlExpiryToStore(urlMeta: Pair<String, CtCacheType>, storePair: Pair<FileStore, InAppAssetsStore>){
            val url = urlMeta.first
            val expiry = System.currentTimeMillis() + EXPIRY_OFFSET_MILLIS
            val fileStore = storePair.first
            val inAppAssetsStore = storePair.second

            when (urlMeta.second) {
                CtCacheType.IMAGE,
                CtCacheType.GIF -> {
                    inAppAssetsStore.saveAssetUrl(url = url, expiry = expiry)
                    fileStore.saveFileUrl(url = url, expiry = expiry)
                }
                CtCacheType.FILES -> {
                    fileStore.saveFileUrl(url = url, expiry = expiry)
                }
            }
        }
    }
    /**
     * Preloads and caches files associated with the given URL-cache type pairs, providing callbacks for completion, success, and failure events.
     *
     * @param urlMeta A list of pairs containing URLs and their corresponding [CtCacheType] values.
     * @param completionCallback A function to be executed when the preloading process is complete, providing a map of URLs and their preloading status (true for success, false for failure).
     * @param successBlock A function to be executed for each URL that is successfully preloaded and cached.
     * @param failureBlock A function to be executed for each URL that fails to preload or cache.
     */
    override fun preloadFilesAndCache(
        urlMeta: List<Pair<String, CtCacheType>>,
        completionCallback: (urlStatusMap: Map<String, Boolean>) -> Unit,
        successBlock: (urlMeta: Pair<String, CtCacheType>) -> Unit,
        failureBlock: (urlMeta: Pair<String, CtCacheType>) -> Unit
    ) {

        val successBlockk: (urlMeta: Pair<String, CtCacheType>) -> Unit = { meta ->
            saveUrlExpiryToStore(meta,Pair(fileStore, inAppAssetsStore))
            updateRepoStatus(
                meta = meta,
                downloadState = DownloadState.SUCCESSFUL
            )
            successBlock.invoke(meta)
        }
        val failureBlockk: (urlMeta: Pair<String, CtCacheType>) -> Unit = { meta ->
            updateRepoStatus(
                meta = meta,
                downloadState = DownloadState.FAILED
            )
            failureBlock.invoke(meta)
        }

        val started : (urlMeta: Pair<String, CtCacheType>) -> Unit = { meta ->
            updateRepoStatus(
                meta = meta,
                downloadState = DownloadState.IN_PROGRESS
            )
        }

        preloaderStrategy.preloadFilesAndCache(
            urlMetas = urlMeta,
            successBlock = successBlockk,
            failureBlock = failureBlockk,
            startedBlock = started,
            preloadFinished = completionCallback
        )
    }

    private fun updateRepoStatus(
        meta: Pair<String, CtCacheType>,
        downloadState: DownloadState
    ) {
        if (urlTriggers.isEmpty()) {
            // added condition to avoid acquiring unnecessary locks
            return
        }
        synchronized(fetchAllFilesLock) {
            downloadInProgressUrls[meta.first] = downloadState
            repoUpdated()
        }
    }
    /**
     * Cleans up stale files based on their expiry time, limiting cleanup to once every 14 days.
     *
     * @param urls A list of valid URLs to exclude from cleanup (if empty, all stale files are cleaned up).
     */
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
    /**
     * Cleans up expired resources associated with the specified cache type.
     *
     * @param cacheTpe The [CtCacheType] for which expired resources should be cleaned up.
     */
    override fun cleanupExpiredResources(cacheTpe: CtCacheType) {
        val allFileUrls = when (cacheTpe) {
            CtCacheType.IMAGE, CtCacheType.GIF -> inAppAssetsStore.getAllAssetUrls()
            CtCacheType.FILES -> fileStore.getAllFileUrls() + inAppAssetsStore.getAllAssetUrls()
        }
        cleanupStaleFilesNow(allFileUrls = allFileUrls)
    }
    /**
     * Cleans up all resources associated with the specified cache type.
     *
     * @param cacheTpe The [CtCacheType] for which all resources should be cleaned up.
     */
    override fun cleanupAllResources(cacheTpe: CtCacheType) {
        val cleanupUrls = when (cacheTpe) {
            CtCacheType.IMAGE,CtCacheType.GIF -> inAppAssetsStore.getAllAssetUrls()
            CtCacheType.FILES -> fileStore.getAllFileUrls() + inAppAssetsStore.getAllAssetUrls()
        }
        cleanupAllFiles(cleanupUrls = cleanupUrls.toList())
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

    private fun repoUpdated() {
        urlTriggers.forEach { dt ->

            val all = dt.urls.all { url ->
                downloadInProgressUrls[url] == DownloadState.SUCCESSFUL
                        || downloadInProgressUrls[url] == DownloadState.FAILED
            }
            if (all) {
                // trigger callback for all downloads finished
                dt.callback.invoke()
            }
        }
    }
}

enum class DownloadState {
    QUEUED, IN_PROGRESS, SUCCESSFUL, FAILED
}

/**
 * Invokes callback once the list of urls passed have finished downloading
 */
data class DownloadTriggerForUrls(
    val urls: List<String>,
    val callback: () -> Unit
)

data class DownloadTriggerResult(
    val successfulUrls: List<String>,
    val failureUrls: List<String>,
    val allSuccessful: Boolean
)