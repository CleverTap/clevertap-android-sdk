package com.clevertap.android.sdk.inapp.images.repo

import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.images.cleanup.FileCleanupStrategy
import com.clevertap.android.sdk.inapp.images.preload.FilePreloaderStrategy
/**
 * A repository interface for managing file resources, such as images or other assets.
 *
 * This interface defines methods for preloading, caching, and cleaning up files, providing a structured approach to
 * handling file resources within an application.
 */
internal interface FileResourcesRepo {
    /**
     * The strategy for cleaning up stale or unnecessary files.
     */
    val cleanupStrategy: FileCleanupStrategy
    /**
     * The strategy for preloading files.
     */
    val preloaderStrategy: FilePreloaderStrategy
    /**
     * Preloads and caches files associated with the given URL-cache type pairs.
     *This method provides default empty callbacks for completion, success, and failure events.
     *
     * @param urlMeta A list of pairs containing URLs and their corresponding [CtCacheType] values.
     */
    fun preloadFilesAndCache(urlMeta: List<Pair<String, CtCacheType>>) = preloadFilesAndCache(urlMeta, {}, {}, {})

    fun preloadFilesAndCache(
        urlMeta: List<Pair<String, CtCacheType>>,
        completionCallback: (urlStatusMap: Map<String, Boolean>) -> Unit
    ) = preloadFilesAndCache(urlMeta, completionCallback, {}, {})

    fun preloadFilesAndCache(
        urlMeta: List<Pair<String, CtCacheType>>,
        completionCallback: (urlStatusMap: Map<String, Boolean>) -> Unit = {},
        successBlock: (urlMeta: Pair<String, CtCacheType>) -> Unit = {},
        failureBlock: (urlMeta: Pair<String, CtCacheType>) -> Unit = {}
    )

    fun cleanupStaleFiles() = cleanupStaleFiles(emptyList())
    /**
     * Cleans up stale files excluding the given URLs.
     *
     * @param urls A list of valid URLs to exclude from cleanup.
     */
    fun cleanupStaleFiles(urls: List<String>)
    /**
     * Cleans up expired resources associated with the specified cache type.
     *
     * @param cacheTpe The [CtCacheType] for which expired resources should be cleaned up.
     */
    fun cleanupExpiredResources(cacheTpe: CtCacheType)
    /**
     * Cleans up all resources associated with the specified cache type.
     *
     * @param cacheTpe The [CtCacheType] for which all resources should be cleaned up.
     */
    fun cleanupAllResources(cacheTpe: CtCacheType)
}