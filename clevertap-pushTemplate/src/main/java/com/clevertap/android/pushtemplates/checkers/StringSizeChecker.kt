package com.clevertap.android.pushtemplates.checkers

import com.clevertap.android.pushtemplates.PTLog

class StringSizeChecker(var entity: String?, var size: Int, var errorMsg: String) :
    SizeChecker<String>(entity, size, errorMsg) {

    override fun check(): Boolean {
        val b = entity?.trim()?.length ?: -1 <= size
        if (b) {
            PTLog.verbose("$errorMsg. Not showing notification")
        }

        return !b
    }
}