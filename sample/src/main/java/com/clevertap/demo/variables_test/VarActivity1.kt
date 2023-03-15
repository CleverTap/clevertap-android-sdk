package com.clevertap.demo.variables_test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.variables.CTVariables
import com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback
import com.clevertap.demo.TestActivity.Companion.flash
import com.clevertap.demo.TestActivity.Companion.toast
import com.clevertap.demo.databinding.ActivityVarBinding
import java.util.*

/*
 * only Define once, only define multiple, only Parse,both
 * oncreate/anytime(i.e after on resume)/ oncreate and anytime
 * - all listeners set before api calls
 *
 * cases for when all apis are called once
 *
 * case 1 : Define [in] onCreate
 * case 2 : Parse  [in] onCreate
 * case 3 : Define [in] anytime
 * case 4 : Parse  [in] anytime
 *
 * case 1 : Parse  [in] onCreate [then] Define [in] onCreate
 * case 2 : Define [in] onCreate [then] Parse  [in] onCreate
 * case 3 : Parse  [in] anyTime  [then] Define [in] anyTime
 * case 4 : Define [in] anyTime  [then] Parse  [in] anyTime
 *
 * case 5 : Parse  [in] onCreate [then] Define [in] anyTime
 * case 6 : Define [in] onCreate [then] Parse  [in] anyTime
 * case 7 : Parse  [in] anyTime  [then] Define [in] onCreate <--- ignore, same as 6
 * case 8 : Define [in] anyTime  [then] Parse  [in] onCreate <--- ignore, same as 5
 *
 * same cases for multiple restarts and app launches
 *
 *
 *  case 2 : Parse  [in] onCreate
 *  - activity launches > listeners get set > parse gets called which calls defineVariable for all variables (> which calls VarCache.register(..)) and init (> which calls varcache.load diffs)
 *    1. when listeners get set, they immediately call their callbacks(check code for reasoning). at that time, Varcache.getVariable(name) returns null for all vars // todo : is it ideal?
 *    2. this implies that variables are not accessible before parse
 *    3. but when accessed after on resume, these variables are available. these are also available when  app is again opened with/without internet
 *  - when when we try to set more listeners in other activities/access variables at any point after parse, these variables become available to those listeners,
 *    1. this indicates that values are available across multiple apps due to common shared RUNTIME cache
 *  - when we try to kill app and open again, we DO NOT GET old values. we get null in listeners
 *    1. this is because storing to permanent cache only happens when response is received from the server
 *
 * case 1 : define  [in] onCreate
 *  - activity launches > listeners get set > parse gets called which calls defineVariable for all variables (> which calls VarCache.register(..)) and init (> which calls varcache.load diffs)
 *    1. when listeners get set, they immediately call their callbacks(check code for reasoning). at that time, Varcache.getVariable(name) returns null for all vars // todo : is it ideal?
 *    2. this implies that variables are not accessible before parse
 *    3. but when accessed after on resume, these variables are available. these are also available when  app is again opened with/without internet
 *  - when when we try to set more listeners in other activities/access variables at any point after parse, these variables become available to those listeners,
 *    1. this indicates that values are available across multiple apps due to common shared RUNTIME cache
 *  - when we try to kill app and open again, we DO NOT GET old values. we get null in listeners
 *    1. this is because storing to permanent cache only happens when response is received from the server
 *
 */

class VarActivity1:AppCompatActivity() {
    private val binding by lazy { ActivityVarBinding.inflate(layoutInflater) }

    private val cleverTapAPI:CleverTapAPI by lazy { CleverTapAPI.getDefaultInstance(this)!! }


    private val multiCallback = object : VariablesChangedCallback() { override fun variablesChanged() { binding.tvTerminalWithGlobalListenerMultiple.flash(getAllVariablesStr()) } }
    private val oneTimeCallback = object : VariablesChangedCallback() { override fun variablesChanged() { binding.tvTerminalWithGlobalListenerOneTime.flash(getAllVariablesStr()) } }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //initVariables
       cleverTapAPI.run {
           addVariablesChangedHandler(multiCallback)
           addOneTimeVariablesChangedHandler(oneTimeCallback)
       }

        //init ui
        binding.run {
            btCheckLocal.setOnClickListener { tvTerminalValueOnDemand.flash(getAllVariablesStr()) }
            btParse.setOnClickListener { parseVariables() }
            btSync.setOnClickListener { syncVariables() }
            btDefineVars.setOnClickListener { defineVariables() }
            btNextActivity.setOnClickListener {  }
        }

        //initial calls
        //parseVariables()
        defineVariables()
    }

    private fun parseVariables(){
        toast("calling:parseVariables")
        cleverTapAPI.parseVariablesForClasses(Vars1::class.java)
    }
    private fun defineVariables(){
        toast("calling:defineVariables")
        Vars1.defineVars1(cleverTapAPI)
    }

    private fun syncVariables(){
        toast("calling:syncVariables")
        cleverTapAPI.pushVariablesToServer()
    }

    private fun getAllVariablesStr():String{
        val str = StringBuilder()
            .appendLine("isDevelopmentMode:${CTVariables.isInDevelopmentMode()} |checked on : ${Date()} " )
            .appendLine("--------------------------------")
            .appendLine("VarActivity1=========================:\n")
            .appendLine("(Parsed----------------)\n")
            .appendLine(Vars1.getParsedVars1(cleverTapAPI))
            .appendLine("(Defined----------------)\n")
            .appendLine(Vars1.getDefinedVars1(cleverTapAPI))

        return str.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleverTapAPI.removeOneTimeVariablesChangedHandler(oneTimeCallback)
        cleverTapAPI.removeVariablesChangedHandler(multiCallback)
    }

}