package com.clevertap.android.sdk.bitmap

import android.content.Context
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.network.DownloadedBitmap
import com.clevertap.android.sdk.network.DownloadedBitmap.Status.NO_IMAGE
import com.clevertap.android.sdk.network.DownloadedBitmap.Status.NO_NETWORK
import com.clevertap.android.sdk.network.DownloadedBitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build


open class BitmapDownloadRequestHandler(private val bitmapDownloader: BitmapDownloader) :
    IBitmapDownloadRequestHandler {

    override fun handleRequest(bitmapDownloadRequest: BitmapDownloadRequest): DownloadedBitmap {
        Logger.v("handling bitmap download request in BitmapDownloadRequestHandler....")

        var srcUrl = bitmapDownloadRequest.bitmapPath
        val context = bitmapDownloadRequest.context

        // If the bitmap path is not specified
        if (srcUrl.isNullOrBlank()) {
            return DownloadedBitmapFactory.nullBitmapWithStatus(NO_IMAGE)
        }

        // Safe bet, won't have more than three /s . url must not be null since we are not handling null pointer exception that would cause otherwise
        srcUrl = srcUrl.replace("///", "/")
            .replace("//", "/")
            .replace("http:/", "http://")
            .replace("https:/", "https://")

        context?.run {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val isOnline = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val activeNetwork = connectivityManager?.activeNetwork
                    if (activeNetwork != null) {
                        val caps = connectivityManager?.getNetworkCapabilities(activeNetwork)
                        caps != null
                                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    }else{
                        @Suppress("DEPRECATION")
                        connectivityManager?.activeNetworkInfo?.isConnected == true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    connectivityManager?.activeNetworkInfo?.isConnected == true
                }
            } catch (e: Exception) {
                Logger.v("Connectivity check failed: ${e.message}")
                false
            }

            if (!isOnline)
            {
                Logger.v("Network connectivity unavailable. Not downloading bitmap. URL was: $srcUrl")
                return DownloadedBitmapFactory.nullBitmapWithStatus(NO_NETWORK)
            }
        }

        return bitmapDownloader.downloadBitmap(srcUrl)
    }
}