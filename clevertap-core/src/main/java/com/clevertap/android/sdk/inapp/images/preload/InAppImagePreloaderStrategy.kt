package com.clevertap.android.sdk.inapp.images.preload

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.images.InAppResourceProvider

internal interface InAppImagePreloaderStrategy {

    val inAppImageProvider: InAppResourceProvider

    val logger: ILogger?

    val config: InAppImagePreloadConfig

    fun preloadImages(urls: List<String>)
    fun cleanup()
}