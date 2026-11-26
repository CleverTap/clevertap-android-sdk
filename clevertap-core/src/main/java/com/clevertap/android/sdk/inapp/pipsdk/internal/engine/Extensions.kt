package com.clevertap.android.sdk.inapp.pipsdk.internal.engine

import android.content.Context
import kotlin.math.roundToInt
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun Int.dpToPx(context: Context): Int =
    (this * context.resources.displayMetrics.density).roundToInt()

/** Converts a percentage (0–100) to pixels given a total dimension in pixels. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun Int.percentOf(total: Int): Int =
    (total * this / 100f).toInt()
