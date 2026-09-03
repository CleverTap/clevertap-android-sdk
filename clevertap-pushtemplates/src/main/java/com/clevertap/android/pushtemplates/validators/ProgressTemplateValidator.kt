package com.clevertap.android.pushtemplates.validators

import com.clevertap.android.pushtemplates.checkers.Checker

/**
 * Validator for the progress-centric (`pt_progress`) template. It has **no required content** — the
 * template always renders a bar (native ProgressStyle on 16+, segmented fallback below) even with a
 * minimal payload — so [loadKeys] is empty and [validate] is always true. (Unlike [ContentValidator],
 * which force-unwraps title/message keys.)
 */
class ProgressTemplateValidator(keys: Map<String, Checker<out Any>>) : Validator(keys) {

    override fun loadKeys(): List<Checker<out Any>> = emptyList()
}
