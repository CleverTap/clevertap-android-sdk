package com.clevertap.android.sdk.inapp.customtemplates

import com.clevertap.android.sdk.CleverTapInstanceConfig

fun interface TemplateProducer {

    /**
     * Returns a set of [CustomTemplate] definitions. [CustomTemplate]s are uniquely identified by their name.
     */
    fun defineTemplates(ctConfig: CleverTapInstanceConfig): Set<CustomTemplate>
}
