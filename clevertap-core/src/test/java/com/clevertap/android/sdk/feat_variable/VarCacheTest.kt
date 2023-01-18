package com.clevertap.android.sdk.feat_variable


import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*

@RunWith(RobolectricTestRunner::class)
class VarCacheTest : BaseTestCase() {


    @Test
    fun test_registerVariable() {
        val vars = listOf(
            Var.define("strVar","123"),
            Var.define("intVar",1),
            Var.define("floatVar",1.1f),
            Var.define("boolVar",false),
            Var.define("arrayVar", arrayOf("books",23,5200.50,true)),
            Var.define("booklist.size",23), //nestedVar
            )


        vars.forEach {
            VarCache.registerVariable(it)
        }
    }

    @Test
    fun test_updateValues() {
    }

    @Test
    fun test_getMergedValueFromComponentArray() {
    }

    @Test
    fun test_testGetMergedValueFromComponentArray() {

    }



    @Test
    fun test_loadDiffs() {
    }

    @Test
    fun test_applyVariableDiffs() {
    }

    @Test
    fun test_mergeHelper() {
    }

    @Test
    fun test_saveDiffs() {
    }

    @Test
    fun test_triggerHasReceivedDiffs() {
    }

    @Test
    fun test_sendContentIfChanged() {
    }

    @Test
    fun test_clearUserContent() {
    }

    @Test
    fun test_setDevModeValuesFromServer() {
    }

    @Test
    fun test_reset() {
    }

    @Test
    fun test_getVariable() {
    }

    @Test
    fun test_setSilent() {
    }

    @Test
    fun test_silent() {
    }

    @Test
    fun test_onUpdate() {
    }

    @Test
    fun test_getDiffs() {
    }

    @Test
    fun test_hasReceivedDiffs() {
    }
}