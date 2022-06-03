package com.clevertap.android.pushtemplates.validators

import com.clevertap.android.pushtemplates.checkers.Checker

abstract class Validator(val keys: Map<String, Checker<out Any>>) {

    abstract fun loadKeys(): List<Checker<out Any>>
    open fun validate(): Boolean {//Does AND check inside Collections.
        return validateKeys()
    }

    open fun validateOR(): Boolean {//Does OR check inside Collections.
        return validateORKeys()
    }



    fun validateKeys(): Boolean {
        val keys = loadKeys()
        return keys.and()
    }

    fun validateORKeys(): Boolean {
        val keys = loadKeys()
        return keys.or()
    }
}