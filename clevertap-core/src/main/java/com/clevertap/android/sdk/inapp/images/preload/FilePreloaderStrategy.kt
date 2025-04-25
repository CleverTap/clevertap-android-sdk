package com.clevertap.android.sdk.inapp.images.preload

import com.clevertap.android.sdk.ILogger
import com.clevertap.android.sdk.inapp.data.CtCacheType
import com.clevertap.android.sdk.inapp.images.FileResourceProvider

internal interface FilePreloaderStrategy {

    val fileResourceProvider: () -> FileResourceProvider

    val logger: ILogger?

    val config: FilePreloadConfig

    val timeoutForPreload: Long

    /**
     * takes list of url meta which contains url string and cachetype to warm up cache after prefetch
     *
     * successBlock - invoked for each successful download
     * failureBlock - invoked for each failed download
     * startedBlock - invoked when download starts for particular url
     */
    fun preloadFilesAndCache(
        urlMetas: List<Pair<String, CtCacheType>>,
        successBlock: (urlMeta: Pair<String, CtCacheType>) -> Unit = {},
        failureBlock: (urlMeta: Pair<String, CtCacheType>) -> Unit = {},
        startedBlock: (urlMeta: Pair<String, CtCacheType>) -> Unit = {},
        preloadFinished: (urlDownloadStatus: Map<String, Boolean>) -> Unit = {}
    )

    fun cleanup()
}