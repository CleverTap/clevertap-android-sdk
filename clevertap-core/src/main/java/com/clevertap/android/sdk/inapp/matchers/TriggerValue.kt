package com.clevertap.android.sdk.inapp.matchers

/**
 * The `TriggerValue` class represents a value used in trigger conditions for in-app messages.
 * It can encapsulate different types of values, including strings, numbers, and lists.
 *
 * @param value The initial value to be encapsulated (default is null).
 * @param listValue The initial list value to be encapsulated (default is null).
 */
class TriggerValue(value: Any? = null, private var listValue: List<*>? = null) {
    private var stringValue: String = ""
    private var numberValue: Number = .0

    /**
     * Initialize the `TriggerValue` object based on the provided value.
     * The object can encapsulate string, number, or list values.
     */
    init {
        when (value) {
            is String -> stringValue = value
            is Number -> numberValue = value
            is List<*> -> listValue = value
        }
    }

    /**
     * Retrieve the encapsulated number value.
     *
     * @return The encapsulated number value, or .0 if the value is not a number.
     */
    fun numberValue(): Number = numberValue

    /**
     * Retrieve the encapsulated string value.
     *
     * @return The encapsulated string value, or blank if the value is not a string.
     */
    fun stringValue(): String = stringValue

    /**
     * Retrieve the encapsulated list value.
     *
     * @return The encapsulated list value, or null if the value is not a list.
     */
    fun listValue(): List<*>? = listValue

    /**
     * Check if the encapsulated value is a list.
     *
     * @return `true` if the encapsulated value is a list, otherwise `false`.
     */
    fun isList(): Boolean = listValue != null

}
