package com.clevertap.android.pushtemplates.validators

import com.clevertap.android.pushtemplates.checkers.Checker
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ContentValidator(keys: Map<String, Checker<out Any>>) : Validator(keys) {

    override fun loadKeys(): List<Checker<out Any>> {
        return listOf(keys[PT_TITLE]!!, keys[PT_MSG]!!)
    }
}
