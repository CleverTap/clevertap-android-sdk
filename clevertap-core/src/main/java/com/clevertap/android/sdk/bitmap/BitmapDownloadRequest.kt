package com.clevertap.android.sdk.bitmap

import androidx.annotation.RestrictTo
import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public data class BitmapDownloadRequest @JvmOverloads constructor(
    var bitmapPath: String?,
    var fallbackToAppIcon: Boolean = false,
    val context: Context? = null,
    val instanceConfig: CleverTapInstanceConfig? = null,
    var downloadTimeLimitInMillis: Long = -1,
    var downloadSizeLimitInBytes: Int = -1,
)
