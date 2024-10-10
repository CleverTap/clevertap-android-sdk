package com.clevertap.android.sdk.inapp.customtemplates

internal class TemplateArgument internal constructor(
    val name: String,
    val type: TemplateArgumentType,
    val defaultValue: Any?
)

internal enum class TemplateArgumentType(private val stringName: String) {
    STRING("string"),
    BOOLEAN("boolean"),
    NUMBER("number"),
    FILE("file"),
    ACTION("action");

    companion object {
        fun fromString(string: String): TemplateArgumentType? {
            return values().find { it.stringName == string }
        }
    }

    override fun toString(): String {
        return stringName;
    }
}
