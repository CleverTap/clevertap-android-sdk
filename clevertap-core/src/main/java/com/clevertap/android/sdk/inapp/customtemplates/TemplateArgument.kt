package com.clevertap.android.sdk.inapp.customtemplates

internal class TemplateArgument internal constructor(
    val name: String,
    val type: TemplateArgumentType,
    val defaultValue: Any?
)

internal enum class TemplateArgumentType(val stringName: String) {
    STRING("string"),
    BOOLEAN("boolean"),
    NUMBER("number"),
    FILE("file"),
    ACTION("action")
}
