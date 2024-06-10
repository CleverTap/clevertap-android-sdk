package com.clevertap.android.sdk.inapp.images.preload

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.images.FileResourceProvider

internal interface FilePreloaderStrategy {

    val fileResourceProvider: FileResourceProvider

    val logger: ILogger?

    val config: FilePreloadConfig

    fun preloadFilesAndCache(
        urlMetas: List<Pair<String, CtCacheType>>,
        successBlock: (urlMeta: Pair<String, CtCacheType>) -> Unit = {},
        failureBlock: (urlMeta: Pair<String, CtCacheType>) -> Unit = {}
    )

    fun cleanup()
}