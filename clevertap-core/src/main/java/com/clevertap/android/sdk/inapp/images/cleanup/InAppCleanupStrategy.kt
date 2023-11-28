package com.clevertap.android.sdk.inapp.images.cleanup

import com.clevertap.android.sdk.inapp.images.InAppResourceProvider

internal interface InAppCleanupStrategy{

    val inAppResourceProvider: InAppResourceProvider

    fun clearAssets(urls: List<String>)

    fun stop()
}