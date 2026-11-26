package com.clevertap.android.sdk

import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun interface FailureFlushListener {

    fun failureFlush(context: Context)
}
