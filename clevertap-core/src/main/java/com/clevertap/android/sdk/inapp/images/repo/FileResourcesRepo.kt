package com.clevertap.android.sdk.inapp.images.repo

import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.images.cleanup.FileCleanupStrategy
import com.clevertap.android.sdk.inapp.images.preload.FilePreloaderStrategy

internal interface FileResourcesRepo {

    val cleanupStrategy: FileCleanupStrategy
    val preloaderStrategy: FilePreloaderStrategy

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
    fun cleanupStaleFiles(urls: List<String>)

    fun cleanupExpiredResources(cacheTpe: CtCacheType)

    fun cleanupAllResources(cacheTpe: CtCacheType)
}