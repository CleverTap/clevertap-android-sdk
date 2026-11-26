package com.clevertap.android.pushtemplates.checkers

import com.clevertap.android.pushtemplates.PTLog
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ListSizeChecker(val entity: List<Any>?, var size: Int, var errorMsg: String) :
    Checker<List<Any>> {

    override fun check(): Boolean {
        val b = entity == null || entity.size < size
        if (b) {
            PTLog.verbose("$errorMsg. Not showing notification")
        }
        return !b
    }
}
