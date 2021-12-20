package com.clevertap.android.pushtemplates.checkers

import com.clevertap.android.pushtemplates.PTLog
import org.json.JSONArray

class JsonArraySizeChecker(val entity: JSONArray?, var size: Int, var errorMsg: String) :
    SizeChecker<JSONArray>(entity, size, errorMsg) {

    override fun check(): Boolean {
        val b = entity == null
        if (b) {
            PTLog.verbose("$errorMsg. Not showing notification")
        }

        return !b
    }
}