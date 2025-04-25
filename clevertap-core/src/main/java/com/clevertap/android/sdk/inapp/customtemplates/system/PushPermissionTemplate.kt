package com.clevertap.android.sdk.inapp.customtemplates.system

import com.clevertap.android.sdk.inapp.InAppActionHandler
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate
import com.clevertap.android.sdk.inapp.customtemplates.function

internal object PushPermissionTemplate {
    private const val NAME = "ctsystem_pushpermission"
    private const val FALLBACK_TO_SETTINGS_ARG = "fbSettings"

    fun createTemplate(systemActionHandler: InAppActionHandler): CustomTemplate {
        return function(isVisual = true) {
            isSystemDefined = true
            name(NAME)
            booleanArgument(FALLBACK_TO_SETTINGS_ARG, false)
            presenter { templateContext ->
                val fbSettings = templateContext.getBoolean(FALLBACK_TO_SETTINGS_ARG) == true

                if (systemActionHandler.launchPushPermissionPrompt(fbSettings)) {
                    templateContext.setPresented()
                }
                templateContext.setDismissed()
            }
        }
    }
}
