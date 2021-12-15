package com.clevertap.android.pushtemplates.validators

import com.clevertap.android.pushtemplates.checkers.Checker

abstract class TemplateValidator(keys: Map<String, Checker<out Any>>) : Validator(keys) {

    abstract override fun validate(): Boolean
}