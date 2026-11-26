package com.clevertap.android.pushtemplates.checkers
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface Checker<T> {

    fun check(): Boolean
}
