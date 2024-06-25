package com.clevertap.android.sdk.inapp.images.cleanup

import com.clevertap.android.sdk.inapp.images.FileResourceProvider

internal interface FileCleanupStrategy{

    val fileResourceProvider: FileResourceProvider

    fun clearFileAssets(urls: List<String>, successBlock: (url: String) -> Unit)

    fun stop()
}