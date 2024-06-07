package com.clevertap.android.sdk.inapp.images.cleanup

import com.clevertap.android.sdk.inapp.images.FileResourceProvider

internal interface FileCleanupStrategy{

    val fileResourceProvider: FileResourceProvider

    fun clearInAppImagesAndGifsV1(urls: List<String>, successBlock: (url: String) -> Unit)
    fun clearFileAssetsV2(urls: List<String>, successBlock: (url: String) -> Unit)

    fun stop()
}