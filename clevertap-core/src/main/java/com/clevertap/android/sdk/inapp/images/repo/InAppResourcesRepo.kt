package com.clevertap.android.sdk.inapp.images.repo

import com.clevertap.android.sdk.inapp.images.cleanup.InAppCleanupStrategy
import com.clevertap.android.sdk.inapp.images.preload.InAppImagePreloaderStrategy

internal interface InAppResourcesRepo {

    val cleanupStrategy: InAppCleanupStrategy
    val preloaderStrategy: InAppImagePreloaderStrategy

    fun fetchAllImages(urls: List<String>)
    fun fetchAllGifs(urls: List<String>)

    fun cleanupStaleImages(validUrls: List<String>)
}