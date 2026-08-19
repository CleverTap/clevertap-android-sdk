package com.clevertap.android.pushtemplates.validators

import com.clevertap.android.pushtemplates.checkers.Checker

/**
 * Validates the keys the pt_custom_rating payload contract marks as required beyond the standard
 * content keys: the rating style and count, and the submit button's label and destination.
 *
 * Per-position assets are not checked here — a single failed asset is substituted with the built-in
 * star pair at render time (R-21), and the renderer decides separately whether enough positions
 * survive to be worth drawing (R-22).
 */
class CustomRatingTemplateValidator(private var validator: Validator) : TemplateValidator(validator.keys) {

    override fun validate(): Boolean {
        return validator.validate() && super.validateKeys()// All check must be true
    }

    override fun loadKeys(): List<Checker<out Any>> {
        return listOf(
            keys[PT_RATING_STYLE]!!,
            keys[PT_RATING_COUNT]!!,
            keys[PT_RATING_CTA_LABEL]!!,
            keys[PT_RATING_CTA_DL]!!,
            keys[PT_RATING_DEFAULT_DL]!!,
        )
    }
}
