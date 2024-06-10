package com.clevertap.android.sdk.inapp.images.repo

import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.images.cleanup.FileCleanupStrategy
import com.clevertap.android.sdk.inapp.images.preload.FilePreloaderStrategy

internal interface FileResourcesRepo {

    val cleanupStrategy: FileCleanupStrategy
    val preloaderStrategy: FilePreloaderStrategy

    fun preloadFilesAndCache(
        urlMeta: List<Pair<String, CtCacheType>>,
    )

    fun cleanupStaleFiles() = cleanupStaleFiles(emptyList())
    fun cleanupStaleFiles(urls: List<String>)

    fun cleanupExpiredInAppsResources()

    fun cleanupInAppsResources()
}