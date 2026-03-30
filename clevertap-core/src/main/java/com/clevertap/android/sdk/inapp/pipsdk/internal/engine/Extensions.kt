package com.clevertap.android.sdk.inapp.pipsdk.internal.engine

import android.content.Context
import kotlin.math.roundToInt

internal fun Int.dpToPx(context: Context): Int =
    (this * context.resources.displayMetrics.density).roundToInt()