package com.clevertap.android.pushtemplates.validators

import com.clevertap.android.pushtemplates.checkers.Checker

abstract class Validator: TemplateValidator {


    private fun loadKeys(): List<Checker<Any>>{

    }

    override fun validate(): Boolean {
        TODO("Not yet implemented")
    }
}