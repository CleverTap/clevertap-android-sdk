package com.clevertap.android.sdk.inapp.media

internal data class InAppMediaConfig(
    val imageViewId: Int,
    val clickableMedia: Boolean,
    val useOrientationForImage: Boolean = true,
    val videoFrameId: Int = 0,
)