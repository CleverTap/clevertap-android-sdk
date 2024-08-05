package com.clevertap.android.sdk.inapp.customtemplates

import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate.FunctionBuilder
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplate.TemplateBuilder

/**
 * Utility function to define a set of [CustomTemplate]s
 */
fun templatesSet(vararg templates: CustomTemplate): Set<CustomTemplate> {
    return setOf(*templates)
}

/**
 * Utility function to build a [CustomTemplate]. Same as using a [TemplateBuilder] directly.
 */
fun template(buildBlock: TemplateBuilder.() -> Unit): CustomTemplate {
    val builder = TemplateBuilder()
    buildBlock(builder)
    return builder.build()
}

/**
 * Utility function to build a [CustomTemplate]. Same as using a [FunctionBuilder] directly.
 */
fun function(isVisual: Boolean, buildBlock: FunctionBuilder.() -> Unit): CustomTemplate {
    val builder = FunctionBuilder(isVisual)
    buildBlock(builder)
    return builder.build()
}
