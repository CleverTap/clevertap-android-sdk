package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.pushnotification.PushProviders

/**
 * Push notification feature
 * Manages push notification providers (FCM, HMS, etc.)
 */
data class PushFeature(
    val pushProviders: PushProviders
)
