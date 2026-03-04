package com.clevertap.android.sdk.network

import android.net.ConnectivityManager
import android.net.NetworkCapabilities

internal fun ConnectivityManager?.isNetworkAvailable(): Boolean {
    return try {
        val activeNetwork = this?.activeNetwork ?: return false
        val capabilities = this.getNetworkCapabilities(activeNetwork) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    } catch (e: Exception) {
        false
    }
}