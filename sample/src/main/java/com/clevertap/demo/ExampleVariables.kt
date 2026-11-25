package com.clevertap.demo

import com.clevertap.android.sdk.variables.annotations.Variable
import com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback

/**
 * Variables registered through `CleverTapAPI.parseVariables(instance)`. The SDK's Parser reads the
 * public fields (Class.getFields()) annotated with [Variable] by reflection, so every variable stays a
 * public JVM field with the same name and primitive type (@JvmField), not a Kotlin property.
 */
class ExampleVariables {

    @JvmField
    @field:Variable
    var var_boolean: Boolean = true

    @JvmField
    @field:Variable
    var var_byte: Byte = 1

    @JvmField
    @field:Variable
    var var_short: Short = 2

    @JvmField
    @field:Variable
    var var_int: Int = 3

    @JvmField
    @field:Variable
    var var_long: Long = 4L

    @JvmField
    @field:Variable
    var var_float: Float = 5f

    @JvmField
    @field:Variable
    var var_double: Double = 6.0

    @JvmField
    @field:Variable
    var var_string: String? = "str"

    @JvmField
    val downloadsCompleteOnce: VariablesChangedCallback = object : VariablesChangedCallback() {
        override fun variablesChanged() {
            println("File downloads finished from downloadsCompleteOnce")
        }
    }

    @JvmField
    val downloadsCompleteRepeated: VariablesChangedCallback = object : VariablesChangedCallback() {
        override fun variablesChanged() {
            println("File downloads finished from downloadsCompleteRepeated")
        }
    }

    val oneTimeVariablesChangedCallback: VariablesChangedCallback = object : VariablesChangedCallback() {
        override fun variablesChanged() {
            println("One Time Variables Changed")
        }
    }

    val variablesChangedCallback: VariablesChangedCallback = object : VariablesChangedCallback() {
        override fun variablesChanged() {
            println("Variables Changed")
        }
    }
}
