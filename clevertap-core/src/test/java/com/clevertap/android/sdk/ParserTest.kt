package com.clevertap.android.sdk

import com.clevertap.android.sdk.feat_variable.Parser
import com.clevertap.android.sdk.feat_variable.Var
import com.clevertap.android.sdk.feat_variable.VarCache
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ParserTest :BaseTestCase(){


    @Test
    fun test_parseVariablesForClasses(){
        println("============before calling Parser.parseVariablesForClasses()")
        checkVarCache()

        Parser.parseVariables(ParserTestInputVariables())

        println("============after calling Parser.parseVariablesForClasses()")
        checkVarCache()
        Assert.assertTrue(true)
    }


    @Test
    fun test_parseVariables(){
        println("============before calling Parser.parseVariablesForClasses()")
        checkVarCache()

        Parser.parseVariablesForClasses(ParserTestInputVariables::class.java)

        println("============after calling Parser.parseVariablesForClasses()")
        checkVarCache()
        Assert.assertTrue(true)
    }



    private fun checkVarCache() {
          println( "checkVarCache() called")
          //println( "checkVarCache:  valuesFromClient:           ${VarCache.valuesFromClient}", )
          println( "checkVarCache:  welcomeMsg:                 ${VarCache.getVariable<String?>("welcomeMsg")}", )
          println( "checkVarCache:  isOptedForOffers:           ${VarCache.getVariable<Boolean?>("isOptedForOffers")}", )
          println( "checkVarCache:  initialCoins:               ${VarCache.getVariable<Int?>("initialCoins")}", )
          println( "checkVarCache:  correctGuessPercentage:     ${VarCache.getVariable<Float?>("correctGuessPercentage")}", )
          println( "checkVariables: userConfigurableProps:      ${VarCache.getVariable<HashMap<String,Any>?>("userConfigurableProps")}" )
          println( "checkVarCache:  aiName                      ${VarCache.getVariable<Array<String>?>("aiName")}", )
    }


    private fun checkVariables() {
        println( "checkVariables() called")
        println( "checkVariables: welcomeMsg: ${ParserTestInputVariables.welcomeMsg}" )
        println( "checkVariables: isOptedForOffers: ${ParserTestInputVariables.isOptedForOffers}" )
        println( "checkVariables: initialCoins: ${ParserTestInputVariables.initialCoins}" )
        println( "checkVariables: correctGuessPercentage: ${ParserTestInputVariables.correctGuessPercentage}" )
        println( "checkVariables: userConfigurableProps: ${ParserTestInputVariables.userConfigurableProps}" )
        println( "checkVariables: aiName: ${(ParserTestInputVariables.aiName)}" )
    }
}