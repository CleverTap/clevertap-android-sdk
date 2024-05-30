package com.clevertap.android.sdk.inapp.images.preload

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.InAppResourceProvider

internal interface FilePreloaderStrategy {

    val inAppImageProvider: InAppResourceProvider

    val logger: ILogger?

    val config: InAppImagePreloadConfig

    fun preloadImages(urls: List<String>) = preloadImages(urls) {}
    fun preloadGifs(urls: List<String>) = preloadGifs(urls) {}
    fun preloadImages(urls: List<String>, successBlock: (url: String) -> Unit = {})
    fun preloadGifs(urls: List<String>, successBlock: (url: String) -> Unit = {})
    fun preloadFiles(
        urls: List<String>,
        successBlock: (url: String) -> Unit = {},
        failureBlock: (url: String) -> Unit = {}
    )
    fun cleanup()
}