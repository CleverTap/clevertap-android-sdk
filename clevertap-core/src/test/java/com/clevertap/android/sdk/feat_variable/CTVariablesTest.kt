package com.clevertap.android.sdk.feat_variable

import com.clevertap.android.sdk.feat_variable.utils.CTVariableUtils.getNameComponents
import org.junit.*

class CTVariablesTest {



    @Test
    fun test_getContext_when_ABC_should_XYZ() {
    }

    @Test
    fun test_setContext_when_ABC_should_XYZ() {
    }

    @Test
    fun test_init_when_ABC_should_XYZ() {
    }

    @Test
    fun test_areVariablesReceived_when_ABC_should_XYZ() {
    }

    @Test
    fun test_clearUserContent_when_ABC_should_XYZ() {
    }

    @Test
    fun test_forceContentUpdate_when_ABC_should_XYZ() {
    }

    @Test
    fun test_addVariablesChangedHandler_when_ABC_should_XYZ() {
    }

    @Test
    fun test_removeVariablesChangedHandler_when_ABC_should_XYZ() {
    }

    @Test
    fun test_addOneTimeVariablesChangedHandler_when_ABC_should_XYZ() {
    }

    @Test
    fun test_removeOneTimeVariablesChangedHandler_when_ABC_should_XYZ() {
    }

    @Test
    fun testgetNameComponents() {
        val testCases = listOf(
            Pair("", arrayOf("")),
            Pair("abc", arrayOf("abc")),
            Pair("abc.def", arrayOf("abc", "def")),
            Pair("abc[def", arrayOf("abc[def")),
            Pair("abc.def[ghi", arrayOf("abc", "def[ghi")),
            Pair("abc.def[ghi.jkl", arrayOf("abc", "def[ghi.jkl")),
            Pair("abc.def[ghi]jkl", arrayOf("abc", "def[ghi]jkl")),
            Pair("abc\\.def", arrayOf("abc.def")),
            Pair("abc[\\.def]", arrayOf("abc[.def]")),
            Pair("abc\\[.ef]", arrayOf("abc", "ef")),
            Pair("abc[.ef]", arrayOf("abc", "ef")),
            Pair("abc\\\\ef]", arrayOf("abc", "ef")),
            Pair("abc(ef]", arrayOf("abc", "ef")),

            )

        for (tc in testCases) {
            val (input, expected) = tc
            val actual = getNameComponents(input)
            if (expected.contentEquals(actual)) {
                println("Test case passed: input=$input, expected=${expected.contentToString()}, actual=${actual.contentToString()}")
            } else {
                println("Test case failed: input=$input, expected=${expected.contentToString()}, actual=${actual.contentToString()}")
            }
        }
    }
}