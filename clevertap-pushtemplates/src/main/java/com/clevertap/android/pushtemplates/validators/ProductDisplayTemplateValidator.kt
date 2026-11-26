package com.clevertap.android.pushtemplates.validators

import com.clevertap.android.pushtemplates.checkers.Checker
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class ProductDisplayTemplateValidator(private var validator: Validator) :
    TemplateValidator(validator.keys) {

    override fun validate(): Boolean {
        return validator.validate() && super.validateKeys()// All check must be true
    }

    override fun loadKeys(): List<Checker<out Any>> {
        return listOf(
            keys[PT_THREE_DEEPLINK_LIST]!!, keys[PT_BIG_TEXT_LIST]!!,
            keys[PT_SMALL_TEXT_LIST]!!, keys[PT_PRODUCT_DISPLAY_ACTION]!!,
            keys[PT_PRODUCT_THREE_IMAGE_LIST]!!
        )
    }
}