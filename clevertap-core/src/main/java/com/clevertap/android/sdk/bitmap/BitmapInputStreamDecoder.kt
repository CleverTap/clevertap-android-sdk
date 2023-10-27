package com.clevertap.android.sdk.bitmap

import android.graphics.BitmapFactory
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.network.DownloadedBitmapFactory
import java.io.InputStream
import java.net.HttpURLConnection

open class BitmapInputStreamDecoder : IBitmapInputStreamReader {

    override fun readInputStream(
        inputStream: InputStream,
        connection: HttpURLConnection,
        downloadStartTimeInMilliseconds: Long
    ): DownloadedBitmap {
        Logger.v("reading bitmap input stream in BitmapInputStreamDecoder....")

        // todo we always assume success
        return DownloadedBitmapFactory.successBitmap(
            bitmap = BitmapFactory.decodeStream(inputStream),
            downloadTime = Utils.getNowInMillis() - downloadStartTimeInMilliseconds
        )
    }
}