package com.clevertap.android.sdk.inapp.customtemplates.system

import android.content.Context
import com.clevertap.android.sdk.inapp.InAppActionHandler
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate
import com.clevertap.android.sdk.inapp.customtemplates.function
import com.clevertap.android.sdk.isNotNullAndBlank

internal object OpenUrlTemplate {

    const val NAME = "Open Url"
    private const val URL_ARG = "Android"

    fun createTemplate(systemActionHandler: InAppActionHandler, context: Context): CustomTemplate {
        return function(isVisual = true) {
            isSystemDefined = true
            name(NAME)
            stringArgument(URL_ARG, "")
            presenter { templateContext ->
                val url = templateContext.getString(URL_ARG)
                if (url.isNotNullAndBlank() && systemActionHandler.openUrl(url, context)) {
                    templateContext.setPresented()
                }
                templateContext.setDismissed()
            }
        }
    }
}
