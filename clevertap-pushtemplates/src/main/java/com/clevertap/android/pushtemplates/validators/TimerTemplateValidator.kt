package com.clevertap.android.pushtemplates.validators

import com.clevertap.android.pushtemplates.checkers.Checker

class TimerTemplateValidator(private var validator: Validator) : TemplateValidator(validator.keys) {

    override fun validate(): Boolean {
        return validator.validate() && super.validateORKeys()// All check must be true
    }

    override fun loadKeys(): List<Checker<out Any>> {
        return listOf(keys[PT_TIMER_THRESHOLD]!!, keys[PT_TIMER_END]!!)
    }
}