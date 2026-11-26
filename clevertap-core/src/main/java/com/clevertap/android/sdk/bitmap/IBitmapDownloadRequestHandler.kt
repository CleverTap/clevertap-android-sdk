package com.clevertap.android.sdk.bitmap

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.network.DownloadedBitmap

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface IBitmapDownloadRequestHandler {
    public fun handleRequest(bitmapDownloadRequest: BitmapDownloadRequest): DownloadedBitmap
}
