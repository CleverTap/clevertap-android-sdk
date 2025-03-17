package com.clevertap.android.sdk.inapp.customtemplates.system

import android.content.Context
import com.clevertap.android.sdk.inapp.InAppActionHandler
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate

internal object SystemTemplates {

    fun getSystemTemplates(
        systemActionHandler: InAppActionHandler,
        context: Context
    ): Set<CustomTemplate> {
        return setOf(OpenUrlTemplate.createTemplate(systemActionHandler, context))
    }
}
