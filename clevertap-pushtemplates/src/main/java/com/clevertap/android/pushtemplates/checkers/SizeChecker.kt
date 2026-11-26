package com.clevertap.android.pushtemplates.checkers
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal abstract class SizeChecker<T>(private var entity: T?, private var size: Int, private var errorMsg: String) :
    Checker<T>