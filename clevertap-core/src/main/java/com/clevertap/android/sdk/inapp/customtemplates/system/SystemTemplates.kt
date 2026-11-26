package com.clevertap.android.sdk.inapp.customtemplates.system

import com.clevertap.android.sdk.inapp.InAppActionHandler
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal object SystemTemplates {

    fun getSystemTemplates(
        systemActionHandler: InAppActionHandler
    ): Set<CustomTemplate> {
        return setOfNotNull(
            OpenUrlTemplate.createTemplate(systemActionHandler),
            PlayStoreAppRatingTemplate.createTemplate(systemActionHandler),
            PushPermissionTemplate.createTemplate(systemActionHandler)
        )
    }
}
