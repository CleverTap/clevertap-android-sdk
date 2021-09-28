package com.clevertap.android.pushtemplates.validators

import com.clevertap.android.pushtemplates.checkers.Checker

abstract class Validator(val keys: Map<String, Checker<out Any>>) {

    abstract fun loadKeys(): List<Checker<out Any>>
    open fun validate(): Boolean {
        return validateKeys()
    }

    fun validateKeys(): Boolean {
        val keys = loadKeys()
        return keys.and()
    }
}