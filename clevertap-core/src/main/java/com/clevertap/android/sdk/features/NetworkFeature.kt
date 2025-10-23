package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.network.ContentFetchManager
import com.clevertap.android.sdk.network.NetworkManager

/**
 * Network and content fetching layer
 * Handles all network operations including the /a1 endpoint
 */
internal data class NetworkFeature(
    val networkManager: NetworkManager,
    val contentFetchManager: ContentFetchManager
)
