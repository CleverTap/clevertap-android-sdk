package com.clevertap.android.pushtemplates.validators

import com.clevertap.android.pushtemplates.checkers.Checker

class RatingTemplateValidator(private var validator: Validator) : TemplateValidator(validator.keys) {

    override fun validate(): Boolean {
        return validator.validate() && super.validateKeys()// All check must be true
    }

    override fun loadKeys(): List<Checker<out Any>> {
        return listOf(keys[PT_DEEPLINK_LIST]!!, keys[PT_RATING_DEFAULT_DL]!!)
    }
}