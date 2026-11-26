package com.clevertap.android.sdk.inapp.media

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal data class InAppMediaConfig(
    val imageViewId: Int,
    val clickableMedia: Boolean,
    val videoFrameId: Int = 0,
    val gifImageId: Int = 0,
)