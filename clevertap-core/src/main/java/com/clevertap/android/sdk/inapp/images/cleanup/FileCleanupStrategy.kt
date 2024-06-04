package com.clevertap.android.sdk.inapp.images.cleanup

import com.clevertap.android.sdk.inapp.images.InAppResourceProvider

internal interface FileCleanupStrategy{

    val inAppResourceProvider: InAppResourceProvider

    fun clearInAppAssets(urls: List<String>, successBlock: (url: String) -> Unit)
    fun clearFileAssets(urls: List<String>, successBlock: (url: String) -> Unit)

    fun stop()
}