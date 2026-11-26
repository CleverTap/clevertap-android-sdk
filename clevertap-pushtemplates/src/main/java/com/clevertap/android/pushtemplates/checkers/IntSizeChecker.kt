package com.clevertap.android.pushtemplates.checkers

import com.clevertap.android.pushtemplates.PTLog
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class IntSizeChecker(val entity: Int?, val size: Int, val errorMsg: String) :
    SizeChecker<Int>(entity, size, errorMsg) {

    override fun check(): Boolean {
        if (entity == null || entity == Int.MIN_VALUE) {
            PTLog.verbose("$errorMsg. Not showing notification")
            return false
        } else {
            val b = entity <= size
            if (b) {
                PTLog.verbose("$errorMsg. Not showing notification")
            }

            return !b
        }
    }
}