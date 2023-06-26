package com.clevertap.android.sdk.bitmap

import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader.HttpBitmapOperation.DOWNLOAD_GZIP_NOTIFICATION_BITMAP_WITH_TIME_LIMIT
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader.HttpBitmapOperation.DOWNLOAD_INAPP_BITMAP
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader.HttpBitmapOperation.DOWNLOAD_NOTIFICATION_BITMAP
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader.HttpBitmapOperation.DOWNLOAD_SIZE_CONSTRAINED_GZIP_NOTIFICATION_BITMAP
import com.clevertap.android.sdk.bitmap.HttpBitmapLoader.HttpBitmapOperation.DOWNLOAD_SIZE_CONSTRAINED_GZIP_NOTIFICATION_BITMAP_WITH_TIME_LIMIT
import com.clevertap.android.sdk.network.DownloadedBitmap

object HttpBitmapLoader {

    private val standardGzipHttpUrlConnectionParams = HttpUrlConnectionParams(
        connectTimeout = Constants.PN_IMAGE_CONNECTION_TIMEOUT_IN_MILLIS,
        readTimeout = Constants.PN_IMAGE_READ_TIMEOUT_IN_MILLIS,
        useCaches = true,
        doInput = true,
        requestMap = mapOf("Accept-Encoding" to "gzip, deflate")
    )
    private val inAppStandardHttpUrlConnectionParams = HttpUrlConnectionParams(
        useCaches = true,
        doInput = true
    )


    enum class HttpBitmapOperation {
        DOWNLOAD_NOTIFICATION_BITMAP,
        DOWNLOAD_GZIP_NOTIFICATION_BITMAP_WITH_TIME_LIMIT,
        DOWNLOAD_SIZE_CONSTRAINED_GZIP_NOTIFICATION_BITMAP,
        DOWNLOAD_SIZE_CONSTRAINED_GZIP_NOTIFICATION_BITMAP_WITH_TIME_LIMIT, DOWNLOAD_INAPP_BITMAP
    }

    @JvmStatic
    fun getHttpBitmap(
        bitmapOperation: HttpBitmapOperation,
        bitmapDownloadRequest: BitmapDownloadRequest
    ): DownloadedBitmap {

        return when (bitmapOperation) {
            DOWNLOAD_NOTIFICATION_BITMAP -> NotificationBitmapDownloadRequestHandler(
                BitmapDownloadRequestHandler(
                    BitmapDownloader(standardGzipHttpUrlConnectionParams, BitmapInputStreamDecoder())
                )
            ).handleRequest(
                bitmapDownloadRequest
            )

            DOWNLOAD_GZIP_NOTIFICATION_BITMAP_WITH_TIME_LIMIT -> {
                val notificationBitmapDownloadRequestHandlerWithTimeLimit = BitmapDownloadRequestHandlerWithTimeLimit(
                    NotificationBitmapDownloadRequestHandler(
                        BitmapDownloadRequestHandler(
                            BitmapDownloader(
                                standardGzipHttpUrlConnectionParams, BitmapInputStreamDecoder(
                                    GzipBitmapInputStreamReader()
                                )
                            )
                        )
                    )
                )
                notificationBitmapDownloadRequestHandlerWithTimeLimit.handleRequest(bitmapDownloadRequest)
            }

            DOWNLOAD_SIZE_CONSTRAINED_GZIP_NOTIFICATION_BITMAP -> NotificationBitmapDownloadRequestHandler(
                BitmapDownloadRequestHandler(
                    BitmapDownloader(
                        standardGzipHttpUrlConnectionParams,
                        BitmapInputStreamDecoder(GzipBitmapInputStreamReader()),
                        Pair(true, bitmapDownloadRequest.downloadSizeLimitInBytes)
                    )
                )
            ).handleRequest(
                bitmapDownloadRequest
            )

            DOWNLOAD_SIZE_CONSTRAINED_GZIP_NOTIFICATION_BITMAP_WITH_TIME_LIMIT -> {
                val notificationBitmapDownloadRequestHandlerWithTimeLimit = BitmapDownloadRequestHandlerWithTimeLimit(
                    NotificationBitmapDownloadRequestHandler(
                        BitmapDownloadRequestHandler(
                            BitmapDownloader(
                                standardGzipHttpUrlConnectionParams,
                                BitmapInputStreamDecoder(GzipBitmapInputStreamReader()),
                                Pair(true, bitmapDownloadRequest.downloadSizeLimitInBytes)
                            )
                        )
                    )
                )
                notificationBitmapDownloadRequestHandlerWithTimeLimit.handleRequest(bitmapDownloadRequest)
            }

            DOWNLOAD_INAPP_BITMAP -> BitmapDownloadRequestHandler(
                BitmapDownloader(
                    inAppStandardHttpUrlConnectionParams, BitmapInputStreamDecoder()
                )
            ).handleRequest(
                bitmapDownloadRequest
            )
        }
    }
}