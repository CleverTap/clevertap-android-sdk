package com.clevertap.android.pushtemplates.checkers

import com.clevertap.android.pushtemplates.PTLog
import org.json.JSONArray
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class JsonArraySizeChecker(val entity: JSONArray?, var size: Int, var errorMsg: String) :
    Checker<JSONArray> {

    override fun check(): Boolean {
        val b = entity == null
        if (b) {
            PTLog.verbose("$errorMsg. Not showing notification")
        }

        return !b
    }
}
