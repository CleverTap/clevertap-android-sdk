package com.clevertap.android.pushtemplates.validators

import com.clevertap.android.pushtemplates.checkers.Checker

class BackgroundValidator(keys: Map<String, Checker<out Any>>) : Validator(keys) {

    override fun loadKeys(): List<Checker<out Any>> {
        return listOf(keys[PT_BG]!!)
    }
}