package com.clevertap.android.pushtemplates.checkers

import com.clevertap.android.pushtemplates.PTLog

class IntSizeChecker(var entity: Int, var size: Int, var errorMsg: String) :
    SizeChecker<Int>(entity, size, errorMsg) {

    override fun check(): Boolean {
        val b = entity == size
        if (b) {
            PTLog.verbose("$errorMsg. Not showing notification")
        }

        return !b
    }
}