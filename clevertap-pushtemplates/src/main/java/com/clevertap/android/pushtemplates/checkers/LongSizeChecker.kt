package com.clevertap.android.pushtemplates.checkers

import com.clevertap.android.pushtemplates.PTLog

class LongSizeChecker(val entity: Long?, val size: Int, val errorMsg: String) :
    SizeChecker<Long>(entity, size, errorMsg) {

    override fun check(): Boolean {
        if (entity == null || entity == Long.MIN_VALUE) {
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