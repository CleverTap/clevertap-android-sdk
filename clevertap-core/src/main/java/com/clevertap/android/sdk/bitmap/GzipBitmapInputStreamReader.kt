package com.clevertap.android.sdk.bitmap

import android.graphics.BitmapFactory
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.network.DownloadedBitmapFactory
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.util.zip.GZIPInputStream

class GzipBitmapInputStreamReader : IBitmapInputStreamReader {

    override fun readInputStream(
        inputStream: InputStream,
        connection: HttpURLConnection,
        downloadStartTimeInMilliseconds: Long
    ): DownloadedBitmap? {

        Logger.v("reading bitmap input stream in GzipBitmapInputStreamReader....")

        val isGZipEncoded = connection.contentEncoding?.contains("gzip") ?: false

        return if (isGZipEncoded) {
            val gzipInputStream = GZIPInputStream(inputStream)

            val bufferForGzipInputStream = ByteArray(16384)
            val decompressedFile = ByteArrayOutputStream()

            var totalBytesRead = 0
            var bytesRead: Int

            // Read data from input stream
            while (gzipInputStream.read(bufferForGzipInputStream).also { bytesRead = it } != -1) {
                totalBytesRead += bytesRead
                decompressedFile.write(bufferForGzipInputStream, 0, bytesRead)
            }

            Logger.v("Total decompressed download size for bitmap from output stream = ${decompressedFile.size()}")

            return getDownloadedBitmapFromStream(decompressedFile, downloadStartTimeInMilliseconds)
        } else null
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