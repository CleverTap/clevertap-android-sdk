package com.clevertap.android.sdk.bitmap

import android.graphics.BitmapFactory
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.network.DownloadedBitmapFactory
import java.io.InputStream
import java.net.HttpURLConnection

class BitmapInputStreamDecoder(
    private val nextBitmapInputStreamReader: GzipBitmapInputStreamReader? = null
) : IBitmapInputStreamReader {

    override fun readInputStream(
        inputStream: InputStream,
        connection: HttpURLConnection,
        downloadStartTimeInMilliseconds: Long
    ): DownloadedBitmap? {
        Logger.v("reading bitmap input stream in BitmapInputStreamDecoder....")

        return nextBitmapInputStreamReader?.readInputStream(
            inputStream = inputStream,
            connection = connection,
            downloadStartTimeInMilliseconds = downloadStartTimeInMilliseconds
        ) ?: DownloadedBitmapFactory.successBitmap(
                bitmap = BitmapFactory.decodeStream(inputStream),
                downloadTime = Utils.getNowInMillis() - downloadStartTimeInMilliseconds
            )
    }
}