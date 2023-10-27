package com.clevertap.android.sdk.bitmap

import android.graphics.BitmapFactory
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.network.DownloadedBitmapFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection

open class BitmapInputStreamDecoder(
    val saveBytes: Boolean = false,
    val logger: Logger? = null
) : IBitmapInputStreamReader {

    override fun readInputStream(
        inputStream: InputStream,
        connection: HttpURLConnection,
        downloadStartTimeInMilliseconds: Long
    ): DownloadedBitmap {
        logger?.verbose("reading bitmap input stream in BitmapInputStreamDecoder....")

        val bufferForHttpInputStream = ByteArray(16384)
        val finalDataFromHttpInputStream = ByteArrayOutputStream()

        var totalBytesRead = 0
        var bytesRead: Int

        // Read data from input stream
        while (inputStream.read(bufferForHttpInputStream).also { bytesRead = it } != -1) {
            totalBytesRead += bytesRead
            finalDataFromHttpInputStream.write(bufferForHttpInputStream, 0, bytesRead)
            logger?.verbose("Downloaded $totalBytesRead bytes")
        }
        logger?.verbose("Total download size for bitmap = $totalBytesRead")

        val dataReadFromStreamInByteArray = finalDataFromHttpInputStream.toByteArray()
        // Decode the bitmap from decompressed data
        val bitmap = BitmapFactory.decodeByteArray(
            dataReadFromStreamInByteArray,
            0,
            dataReadFromStreamInByteArray.size
        )

        val fileLength = connection.contentLength
        if (fileLength != -1 && fileLength != totalBytesRead) {
            logger?.debug("File not loaded completely not going forward. URL was: ${connection.url}")
            return DownloadedBitmapFactory.nullBitmapWithStatus(DownloadedBitmap.Status.DOWNLOAD_FAILED)
        }

        return DownloadedBitmapFactory.successBitmap(
            bitmap = bitmap,
            downloadTime = Utils.getNowInMillis() - downloadStartTimeInMilliseconds,
            data = if (saveBytes) {
                dataReadFromStreamInByteArray
            } else {
                null
            }
        )
    }
}