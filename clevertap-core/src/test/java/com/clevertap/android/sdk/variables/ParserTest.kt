package com.clevertap.android.sdk.variables

import com.clevertap.android.sdk.inapp.images.FileResourceProvider
import com.clevertap.android.sdk.inapp.images.repo.FileResourcesRepoImpl
import com.clevertap.android.sdk.variables.VariableDefinitions.TestVarsJI
import com.clevertap.android.shared.test.BaseTestCase
import io.mockk.*
import org.junit.Test
import java.lang.reflect.Field

class ParserTest:BaseTestCase() {

    private lateinit var parser: Parser
    private lateinit var parserSpy: Parser

    private lateinit var ctv:CTVariables
    private lateinit var ctvSpy: CTVariables

    private  lateinit var varCache:VarCache
    private lateinit var fileResourcesRepoImpl: FileResourcesRepoImpl
    private lateinit var fileResourceProvider: FileResourceProvider

    override fun setUp() {
        super.setUp()

        fileResourcesRepoImpl = mockk(relaxed = true)
        fileResourceProvider = mockk(relaxed = true)
        varCache = VarCache(
            cleverTapInstanceConfig,
            appCtx,
            fileResourcesRepoImpl
        )

        ctv = CTVariables(varCache)
        ctvSpy = spyk(ctv)

        parser = Parser(ctvSpy)
        parserSpy = spyk(parser)

    }

    @Test
    fun parseVariables() {
        val inst1 = TestVarsJI()
        val inst2 = TestVarsJI()
        parserSpy.parseVariables(inst1,inst2)
        verify(exactly = 1) { parserSpy.parseVariablesHelper(inst1, TestVarsJI::class.java) }
        verify(exactly = 1) { parserSpy.parseVariablesHelper(inst2, TestVarsJI::class.java) }
    }

    @Test
    fun parseVariablesForClasses() {
        parserSpy.parseVariablesForClasses(
            VariableDefinitions::class.java,
            VariableDefinitions::class.java)
        verify(exactly = 2) {
            parserSpy.parseVariablesHelper(
                null,
                VariableDefinitions::class.java
            )
        }
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
        verify(exactly = 1) {
            parserSpy.defineVariable(
                case1.second,
                "welcomeMsg",
                "Hello User",
                CTVariableUtils.STRING,
                field("welcomeMsg")
            )
        }
        verify(exactly = 1) {
            parserSpy.defineVariable(
                case1.second,
                "isOptedForOffers",
                true,
                CTVariableUtils.BOOLEAN,
                field("isOptedForOffers")
            )
        }
        verify(exactly = 1) {
            parserSpy.defineVariable(
                case1.second,
                "initialCoins",
                45,
                CTVariableUtils.NUMBER,
                field("initialCoins")
            )
        }
        verify(exactly = 1) {
            parserSpy.defineVariable(
                case1.second,
                "correctGuessPercentage",
                50.0F,
                CTVariableUtils.NUMBER,
                field("correctGuessPercentage")
            )
        }
        verify(exactly = 0) { ctvSpy.init() } // init is called only once when CleverTapAPI instance is created
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
        verify(exactly = 1) {
            parserSpy.defineVariable(
                case2.second,
                "javaIStr",
                "code_string",
                CTVariableUtils.STRING,
                field("javaIStr")
            )
        }
        verify(exactly = 1) {
            parserSpy.defineVariable(
                case2.second,
                "javaIBool",
                false,
                CTVariableUtils.BOOLEAN,
                field("javaIBool")
            )
        }
        verify(exactly = 1) {
            parserSpy.defineVariable(
                case2.second,
                "javaIInt",
                1.0,
                CTVariableUtils.NUMBER,
                field("javaIInt")
            )
        }
        verify(exactly = 1) {
            parserSpy.defineVariable(
                case2.second,
                "javaIDouble",
                1.42,
                CTVariableUtils.NUMBER,
                field("javaIDouble")
            )
        }
        verify(exactly = 0) { ctvSpy.init() } // init is called only once when CleverTapAPI instance is created
    }
}
