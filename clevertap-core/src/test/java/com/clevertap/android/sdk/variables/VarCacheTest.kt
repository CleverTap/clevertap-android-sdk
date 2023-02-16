package com.clevertap.android.sdk.variables


import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*
import kotlin.test.assertTrue

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
        val name = "mobile.smartphone.android.samsung.s22"
        val components = CTVariableUtils.getNameComponents(name)
        println("components=${Arrays.toString(components)}")
        val value = 54999.99
        val kind = "float"
        val values = hashMapOf<String,Any?>()
        val kinds= hashMapOf<String,String>()
        CTVariableUtils.updateValuesAndKinds(name,components,value, kind, values, kinds)
        println("a: new values = $values") // {mobile={smartphone={android={samsung={s22=54999.99}}}}}
        println("a: new kinds = $kinds")  //  {"mobile.smartphone.android.samsung.s22"=float}

        val name2 = "mobile.smartphone.apple.iphone.15pro"
        val components2= CTVariableUtils.getNameComponents(name2)
        val value2 = "unreleased"
        CTVariableUtils.updateValuesAndKinds(name2,components2,value2, kind, values, kinds)
        println("b: new values = $values") //{mobile={smartphone={apple={iphone={15pro=unreleased}}, android={samsung={s22=54999.99}}}}}
        println("b: new kinds = $kinds")  //{mobile.smartphone.android.samsung.s22=float, mobile.smartphone.apple.iphone.15pro=float}

        assertTrue (true)
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