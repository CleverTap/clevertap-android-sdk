package com.clevertap.android.sdk.inapp.images.repo

import com.clevertap.android.sdk.inapp.images.cleanup.FileCleanupStrategy
import com.clevertap.android.sdk.inapp.images.preload.FilePreloaderStrategy

internal interface FileResourcesRepo {

    val cleanupStrategy: FileCleanupStrategy
    val preloaderStrategy: FilePreloaderStrategy

    fun fetchAllInAppImagesV1(urls: List<String>)
    fun fetchAllInAppGifsV1(urls: List<String>)
    fun fetchAllFiles(
        urls: List<String>,
        completionCallback: (status: Boolean, urlStatusMap: Map<String, Boolean>) -> Unit
    )

    fun cleanupStaleInAppImagesAndGifsV1(validUrls: List<String>)
    fun cleanupStaleFiles(validUrls: List<String>)
}