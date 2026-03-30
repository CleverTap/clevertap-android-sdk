package com.clevertap.android.sdk.inapp.pipsdk.internal.engine

import android.content.Context

internal fun Int.dpToPx(context: Context): Int =
    (this * context.resources.displayMetrics.density + 0.5f).toInt()