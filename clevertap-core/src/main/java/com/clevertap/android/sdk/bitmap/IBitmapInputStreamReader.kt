package com.clevertap.android.sdk.bitmap

import com.clevertap.android.sdk.network.DownloadedBitmap
import java.io.InputStream
import java.net.HttpURLConnection

interface IBitmapInputStreamReader {

    fun readInputStream(
        inputStream: InputStream,
        connection: HttpURLConnection,
        downloadStartTimeInMilliseconds: Long
    ): DownloadedBitmap?
}