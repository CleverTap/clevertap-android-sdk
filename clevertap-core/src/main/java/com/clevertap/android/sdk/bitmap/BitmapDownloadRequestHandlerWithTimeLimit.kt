package com.clevertap.android.sdk.bitmap

import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.network.DownloadedBitmap.Status.DOWNLOAD_FAILED
import com.clevertap.android.sdk.network.DownloadedBitmapFactory
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.task.Task

class BitmapDownloadRequestHandlerWithTimeLimit(
    private val iBitmapDownloadRequestHandler: IBitmapDownloadRequestHandler
) : IBitmapDownloadRequestHandler {

    override fun handleRequest(bitmapDownloadRequest: BitmapDownloadRequest): DownloadedBitmap {

        Logger.verbose("handling bitmap download request in BitmapDownloadRequestHandlerWithTimeLimit....")

        val (_, fallbackToAppIcon, context, instanceConfig, downloadTimeLimitInMillis) = bitmapDownloadRequest

        if (instanceConfig == null || downloadTimeLimitInMillis == -1L) {
            Logger.verbose("either config is null or downloadTimeLimitInMillis is negative.")
            Logger.verbose("will download bitmap without time limit")
            return iBitmapDownloadRequestHandler.handleRequest(bitmapDownloadRequest)
        }

        val bitmapDownloadTask: Task<DownloadedBitmap> = CTExecutorFactory.executors(instanceConfig).ioTask()

        var downloadedBitmap: DownloadedBitmap? = bitmapDownloadTask.submitAndGetResult(
            "getNotificationBitmap",
            { iBitmapDownloadRequestHandler.handleRequest(bitmapDownloadRequest) },
            downloadTimeLimitInMillis
        )

        if (downloadedBitmap == null) { // in case some exception in executor framework we get null result from future.
            downloadedBitmap = DownloadedBitmapFactory.nullBitmapWithStatus(DOWNLOAD_FAILED)
        }

        return Utils.getDownloadedBitmapPostFallbackIconCheck(fallbackToAppIcon, context, downloadedBitmap)
    }
}