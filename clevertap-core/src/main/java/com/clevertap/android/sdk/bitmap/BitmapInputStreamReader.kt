package com.clevertap.android.sdk.bitmap

import android.graphics.BitmapFactory
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.network.DownloadedBitmap.Status.DOWNLOAD_FAILED
import com.clevertap.android.sdk.network.DownloadedBitmapFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection

class BitmapInputStreamReader(
    private val nextBitmapInputStreamReader: IBitmapInputStreamReader? = null,
    private val checkDownloadCompleteness: Boolean = false
) : IBitmapInputStreamReader {

    override fun readInputStream(
        inputStream: InputStream,
        connection: HttpURLConnection,
        downloadStartTimeInMilliseconds: Long
    ): DownloadedBitmap? {

        Logger.v("reading bitmap input stream in BitmapInputStreamReader....")
        val bufferForHttpInputStream = ByteArray(16384)
        val finalDataFromHttpInputStream = ByteArrayOutputStream()

        var totalBytesRead = 0
        var bytesRead: Int

        // Read data from input stream
        while (inputStream.read(bufferForHttpInputStream).also { bytesRead = it } != -1) {
            totalBytesRead += bytesRead
            finalDataFromHttpInputStream.write(bufferForHttpInputStream, 0, bytesRead)
            Logger.v("Downloaded $totalBytesRead bytes")
        }
        Logger.v("Total download size for bitmap = $totalBytesRead")

        if (checkDownloadCompleteness) {
            // might be -1: server did not report the length
            val fileLength = connection.contentLength
            if (fileLength != -1 && fileLength != totalBytesRead) {
                Logger.d("File not loaded completely not going forward. URL was: ${connection.url}")
                return DownloadedBitmapFactory.nullBitmapWithStatus(DOWNLOAD_FAILED)
            }
        }

        return nextBitmapInputStreamReader?.readInputStream(
            ByteArrayInputStream(finalDataFromHttpInputStream.toByteArray()),
            connection,
            downloadStartTimeInMilliseconds
        ) ?: getDownloadedBitmapFromStream(finalDataFromHttpInputStream, downloadStartTimeInMilliseconds)
    }

    private fun getDownloadedBitmapFromStream(
        dataReadFromStream: ByteArrayOutputStream, downloadStartTimeInMilliseconds: Long
    ): DownloadedBitmap {

        val dataReadFromStreamInByteArray = dataReadFromStream.toByteArray()
        // Decode the bitmap from decompressed data
        val bitmap =
            BitmapFactory.decodeByteArray(dataReadFromStreamInByteArray, 0, dataReadFromStreamInByteArray.size)
        return DownloadedBitmapFactory.successBitmap(
            bitmap, Utils.getNowInMillis() - downloadStartTimeInMilliseconds
        )
    }
}