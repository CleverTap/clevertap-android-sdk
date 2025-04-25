package com.clevertap.android.sdk.inapp.customtemplates.system

import com.clevertap.android.sdk.inapp.InAppActionHandler
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate
import com.clevertap.android.sdk.inapp.customtemplates.function

internal object PlayStoreAppRatingTemplate {

    private const val NAME = "ctsystem_apprating"

    fun createTemplate(systemActionHandler: InAppActionHandler): CustomTemplate? {
        if (!systemActionHandler.isPlayStoreReviewLibraryAvailable()) {
            return null
        }
        return function(isVisual = true) {
            isSystemDefined = true
            name(NAME)
            presenter { templateContext ->
                systemActionHandler.launchPlayStoreReviewFlow(
                    onCompleted = {
                        templateContext.setPresented()
                        templateContext.setDismissed()
                    },
                    onError = { templateContext.setDismissed() }
                )
            }
        }
    }
}
