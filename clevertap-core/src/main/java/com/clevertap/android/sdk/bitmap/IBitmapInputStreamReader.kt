package com.clevertap.android.sdk.bitmap

import androidx.annotation.RestrictTo
import com.clevertap.android.sdk.network.DownloadedBitmap
import java.io.InputStream
import java.net.HttpURLConnection

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface IBitmapInputStreamReader {

    public fun readInputStream(
        inputStream: InputStream,
        connection: HttpURLConnection,
        downloadStartTimeInMilliseconds: Long
    ): DownloadedBitmap
}