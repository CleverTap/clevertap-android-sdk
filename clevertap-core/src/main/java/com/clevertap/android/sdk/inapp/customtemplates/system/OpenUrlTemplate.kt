package com.clevertap.android.sdk.inapp.customtemplates.system

import com.clevertap.android.sdk.inapp.InAppActionHandler
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate
import com.clevertap.android.sdk.inapp.customtemplates.function
import com.clevertap.android.sdk.isNotNullAndBlank

internal object OpenUrlTemplate {

    private const val NAME = "ctsystem_openurl"
    private const val URL_ARG = "Android"

    fun createTemplate(systemActionHandler: InAppActionHandler): CustomTemplate {
        return function(isVisual = true) {
            isSystemDefined = true
            name(NAME)
            stringArgument(URL_ARG, "")
            presenter { templateContext ->
                val url = templateContext.getString(URL_ARG)
                if (url.isNotNullAndBlank() && systemActionHandler.openUrl(url)) {
                    templateContext.setPresented()
                }
                templateContext.setDismissed()
            }
        }
    }
}
