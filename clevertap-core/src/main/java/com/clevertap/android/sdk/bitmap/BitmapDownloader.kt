package com.clevertap.android.sdk.bitmap

import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.network.DownloadedBitmap.Status.DOWNLOAD_FAILED
import com.clevertap.android.sdk.network.DownloadedBitmap.Status.SIZE_LIMIT_EXCEEDED
import com.clevertap.android.sdk.network.DownloadedBitmapFactory
import java.net.HttpURLConnection
import java.net.URL

class BitmapDownloader(
    private val httpUrlConnectionParams: HttpUrlConnectionParams,
    private val bitmapInputStreamReader: IBitmapInputStreamReader,
    private val sizeConstrainedPair: Pair<Boolean, Int> = Pair(false, 0)
) {

    private var downloadStartTimeInMilliseconds: Long = 0
    private lateinit var connection: HttpURLConnection
    private lateinit var srcUrl: String

    fun downloadBitmap(srcUrl: String): DownloadedBitmap {
        Logger.v("initiating bitmap download in BitmapDownloader....")

        this.srcUrl = srcUrl
        downloadStartTimeInMilliseconds = Utils.getNowInMillis()
        try {
            connection = URL(srcUrl).run { createConnection(this) }
            connection.run {
                connect()

                // expect HTTP 200 OK, so we don't mistakenly save error report instead of the file
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Logger.d("File not loaded completely not going forward. URL was: $srcUrl")
                    return DownloadedBitmapFactory.nullBitmapWithStatus(DOWNLOAD_FAILED)
                }

                Logger.v("Downloading $srcUrl....")

                // might be -1: server did not report the length
                val fileLength = contentLength
                val (isSizeConstrained, size) = sizeConstrainedPair

                // Check if the size limit is exceeded
                if (isSizeConstrained && fileLength > size) {
                    Logger.v("Image size is larger than $size bytes. Cancelling download!")
                    return DownloadedBitmapFactory.nullBitmapWithStatus(SIZE_LIMIT_EXCEEDED)
                }

                return bitmapInputStreamReader.readInputStream(
                    inputStream = inputStream,
                    connection = this,
                    downloadStartTimeInMilliseconds = downloadStartTimeInMilliseconds
                )
            }
        } catch (e: Throwable) {
            Logger.v("Couldn't download the notification icon. URL was: $srcUrl")
            e.printStackTrace()
            return DownloadedBitmapFactory.nullBitmapWithStatus(DOWNLOAD_FAILED)
        } finally {
            try {
                connection.disconnect()
            } catch (t: Throwable) {
                Logger.v("Couldn't close connection!", t)
            }
        }
    }

    private fun createConnection(url: URL): HttpURLConnection =
        (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = httpUrlConnectionParams.connectTimeout
            readTimeout = httpUrlConnectionParams.readTimeout
            useCaches = httpUrlConnectionParams.useCaches
            doInput = httpUrlConnectionParams.doInput
            httpUrlConnectionParams.requestMap.forEach { (k, v) -> addRequestProperty(k, v) }
        }

}