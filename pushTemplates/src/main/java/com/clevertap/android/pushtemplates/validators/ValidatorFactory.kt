package com.clevertap.android.pushtemplates.validators

import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.TemplateType
import com.clevertap.android.pushtemplates.checkers.Checker

class ValidatorFactory {

    private lateinit var keys: Map<String, Checker<Any>>
    private var keysLoaded: Boolean = false

    private fun getValidator(templateType: TemplateType, templateRenderer: TemplateRenderer): Validator {

    }

    private fun createKeysMap(templateRenderer: TemplateRenderer): Map<String, Checker<Any>>{

    }
}