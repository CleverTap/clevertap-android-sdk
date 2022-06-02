package com.clevertap.android.pushtemplates.checkers

import com.clevertap.android.pushtemplates.PTLog

class IntSizeChecker(var entity: Int, var size: Int, var errorMsg: String) :
    SizeChecker<Int>(entity, size, errorMsg) {

    override fun check(): Boolean {
        if (entity == Int.MIN_VALUE){
            PTLog.verbose("Timer End Value not defined. Not showing notification")
            return false
        }else {
            val b = entity <= size
            if (b) {
                PTLog.verbose("$errorMsg. Not showing notification")
            }

            return !b
        }
    }
}