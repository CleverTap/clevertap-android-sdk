package com.clevertap.android.pushtemplates

import com.clevertap.android.sdk.bitmap.BitmapDownloadRequest
import com.clevertap.android.sdk.bitmap.BitmapDownloadRequestHandler
import com.clevertap.android.sdk.bitmap.BitmapDownloadRequestHandlerWithTimeLimit
import com.clevertap.android.sdk.bitmap.BitmapDownloader
import com.clevertap.android.sdk.bitmap.BitmapInputStreamDecoder
import com.clevertap.android.sdk.bitmap.HttpUrlConnectionParams
import com.clevertap.android.sdk.network.DownloadedBitmap


object PTHttpBitmapLoader {
    private const val CONNECT_TIMEOUT_MS = 1000
    private const val READ_TIMEOUT_MS = 5000

    enum class PTHttpBitmapOperation {
        DOWNLOAD_GIF_BYTES_WITH_TIME_LIMIT
    }

    private val standardGzipHttpUrlConnectionParams = HttpUrlConnectionParams(
        connectTimeout = CONNECT_TIMEOUT_MS,
        readTimeout = READ_TIMEOUT_MS,
        useCaches = true,
        doInput = true,
        requestMap = mapOf(
            "Accept-Encoding" to "gzip, deflate",
        )
    )

    @JvmStatic
    fun getHttpBitmap(
        bitmapOperation: PTHttpBitmapOperation,
        bitmapDownloadRequest: BitmapDownloadRequest
    ): DownloadedBitmap {
        return when (bitmapOperation) {
            PTHttpBitmapOperation.DOWNLOAD_GIF_BYTES_WITH_TIME_LIMIT -> {
                BitmapDownloadRequestHandlerWithTimeLimit(
                    BitmapDownloadRequestHandler(
                        bitmapDownloader = BitmapDownloader(
                            httpUrlConnectionParams = standardGzipHttpUrlConnectionParams,
                            bitmapInputStreamReader = BitmapInputStreamDecoder(
                                saveBitmap = false,
                                saveBytes = true
                            )
                        )
                    )
                ).handleRequest(
                    bitmapDownloadRequest = bitmapDownloadRequest
                )
            }

        }
    }
}
