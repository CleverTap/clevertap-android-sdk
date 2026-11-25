package com.clevertap.android.sdk.variables

import com.clevertap.android.sdk.variables.annotations.Variable

/**
 * Variable holders read by [Parser] through reflection (`Class.getFields()`).
 *
 * `object` + `@JvmField` keeps these as public **static** fields with the original names (the
 * parser reads them with a null instance); the nested holder classes keep **instance** fields.
 */
object VariableDefinitions {
    @Variable @JvmField var welcomeMsg: String = "Hello User"
    @Variable @JvmField var isOptedForOffers: Boolean = true
    @Variable @JvmField var initialCoins: Int = 45
    @Variable @JvmField var correctGuessPercentage: Float = 50.0f

    class TestVarsJI {
        @Variable @JvmField var javaIStr: String = "code_string"
        @Variable @JvmField var javaIDouble: Double = 1.42
        @Variable @JvmField var javaIInt: Double = 1.0
        @Variable @JvmField var javaIBool: Boolean = false
    }

    class NullDefaultValue {
        @Variable @JvmField var string_with_null: String? = null
    }

    object Groups {
        @Variable(group = "group1") @JvmField var var_string1: String = "str1"

        @Variable
        @JvmField
        var group1: MutableMap<String, Any> = hashMapOf(
            "var_int1" to 1,
            "var_string2" to "str2",
            // nested map
            "group2" to hashMapOf<String, Any>("var_int2" to 2)
        )

        @Variable @JvmField var var_int3: Int = 3
    }
}
