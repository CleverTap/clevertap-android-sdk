package com.clevertap.android.sdk.inapp.customtemplates

import com.clevertap.android.sdk.CleverTapInstanceConfig

/**
 * Supplies [CustomTemplate] definitions to the SDK. Register a producer with
 * [com.clevertap.android.sdk.CleverTapAPI.registerCustomInAppTemplates] before creating a
 * CleverTap instance.
 */
public fun interface TemplateProducer {

    /**
     * Returns a set of [CustomTemplate] definitions. [CustomTemplate]s are uniquely identified by their name.
     */
    public fun defineTemplates(ctConfig: CleverTapInstanceConfig): Set<CustomTemplate>
}
