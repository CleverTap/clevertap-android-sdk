package com.clevertap.android.sdk.inapp.images.preload

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.FileResourceProvider

internal interface FilePreloaderStrategy {

    val fileResourceProvider: FileResourceProvider

    val logger: ILogger?

    val config: FilePreloadConfig

    fun preloadInAppImagesV1(urls: List<String>) = preloadInAppImagesV1(urls) {}
    fun preloadInAppGifsV1(urls: List<String>) = preloadInAppGifsV1(urls) {}
    fun preloadInAppImagesV1(urls: List<String>, successBlock: (url: String) -> Unit = {})
    fun preloadInAppGifsV1(urls: List<String>, successBlock: (url: String) -> Unit = {})
    fun preloadFiles(
        urls: List<String>,
        successBlock: (url: String) -> Unit = {},
        failureBlock: (url: String) -> Unit = {}
    )
    fun cleanup()
}