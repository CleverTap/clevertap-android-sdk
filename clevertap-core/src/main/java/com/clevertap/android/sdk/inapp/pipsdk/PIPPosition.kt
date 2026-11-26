package com.clevertap.android.sdk.inapp.pipsdk

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal enum class PIPPosition {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    LEFT_CENTER, CENTER, RIGHT_CENTER,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}
