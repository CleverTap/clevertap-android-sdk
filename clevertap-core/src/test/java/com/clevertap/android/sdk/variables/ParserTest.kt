package com.clevertap.android.sdk.variables

import com.clevertap.android.sdk.variables.VariableDefinitions.TestVarsJI
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Field

@RunWith(RobolectricTestRunner::class)
class ParserTest:BaseTestCase() {

    private lateinit var parser: Parser
    private lateinit var parserSpy: Parser

    private lateinit var ctv:CTVariables
    private lateinit var ctvSpy: CTVariables

    private  lateinit var varCache:VarCache
    override fun setUp() {
        super.setUp()

        varCache = VarCache(cleverTapInstanceConfig,appCtx)

        ctv = CTVariables(varCache)
        ctvSpy = Mockito.spy(ctv)

        parser = Parser(ctvSpy)
        parserSpy = Mockito.spy(parser)

    }

    @Test
    fun parseVariables() {
        val inst1 = TestVarsJI()
        val inst2 = TestVarsJI()
        parserSpy.parseVariables(inst1,inst2)
        Mockito.verify(parserSpy,Mockito.times(1)).parseVariablesHelper(inst1,TestVarsJI::class.java)
        Mockito.verify(parserSpy,Mockito.times(1)).parseVariablesHelper(inst2,TestVarsJI::class.java)
    }

    @Test
    fun parseVariablesForClasses() {
        parserSpy.parseVariablesForClasses(
            VariableDefinitions::class.java,
            VariableDefinitions::class.java)
        Mockito.verify(parserSpy,Mockito.times(2)).parseVariablesHelper(null,
            VariableDefinitions::class.java)
    }

    @Test
    fun parseVariablesHelper_case1(){
        val case1 = Pair(VariableDefinitions::class.java , null)

        //test
        parserSpy.parseVariablesHelper(case1.second,case1.first)

        val fields = case1.first.fields
        fun field(key:String):Field{
            return fields.first { it.name.equals(key) }
        }

        //verify
        Mockito.verify(parserSpy,Mockito.times(1)).defineVariable(case1.second,"welcomeMsg","Hello User",CTVariableUtils.STRING,field("welcomeMsg"))
        Mockito.verify(parserSpy,Mockito.times(1)).defineVariable(case1.second, "isOptedForOffers", true, CTVariableUtils.BOOLEAN, field("isOptedForOffers"))
        Mockito.verify(parserSpy,Mockito.times(1)).defineVariable(case1.second, "initialCoins", 45, CTVariableUtils.NUMBER, field("initialCoins"))
        Mockito.verify(parserSpy,Mockito.times(1)).defineVariable(case1.second, "correctGuessPercentage", 50.0F, CTVariableUtils.NUMBER, field("correctGuessPercentage"))
        Mockito.verify(ctvSpy,Mockito.times(0)).init() // init is called only once when CleverTapAPI instance is created
    }

    @Test
    fun parseVariablesHelper_case2(){
        val case2 = Pair(TestVarsJI::class.java , TestVarsJI())

        //test
        parserSpy.parseVariablesHelper(case2.second,case2.first)

        val fields = case2.first.fields
        fun field(key:String):Field{
            return fields.first { it.name.equals(key) }
        }

        //verify
        Mockito.verify(parserSpy,Mockito.times(1)).defineVariable(case2.second,"javaIStr","code_string",CTVariableUtils.STRING,field("javaIStr"))
        Mockito.verify(parserSpy,Mockito.times(1)).defineVariable(case2.second, "javaIBool", false, CTVariableUtils.BOOLEAN, field("javaIBool"))
        Mockito.verify(parserSpy,Mockito.times(1)).defineVariable(case2.second, "javaIInt", 1.0, CTVariableUtils.NUMBER, field("javaIInt"))
        Mockito.verify(parserSpy,Mockito.times(1)).defineVariable(case2.second, "javaIDouble", 1.42, CTVariableUtils.NUMBER, field("javaIDouble"))
        Mockito.verify(ctvSpy,Mockito.times(0)).init() // init is called only once when CleverTapAPI instance is created

    }
}
