package com.clevertap.android.pushtemplates.checkers

import com.clevertap.android.pushtemplates.PTLog

class ListSizeChecker(val entity: List<Any>?, var size: Int, var errorMsg: String) :
    SizeChecker<List<Any>>(entity, size, errorMsg) {

    override fun check(): Boolean {
        val b = entity == null || entity.size < size
        if (b) {
            PTLog.verbose("$errorMsg. Not showing notification")
        }
        return !b
    }
}