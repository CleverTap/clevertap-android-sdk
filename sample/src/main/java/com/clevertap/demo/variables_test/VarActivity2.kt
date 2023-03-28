package com.clevertap.demo.variables_test

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback
import com.clevertap.demo.databinding.ActivityVarBinding
import com.clevertap.demo.variables_test.models.Vars2

class VarActivity2:AppCompatActivity() {
    private val binding by lazy { ActivityVarBinding.inflate(layoutInflater) }

    private val cleverTapAPI: CleverTapAPI by lazy { CleverTapAPI.getDefaultInstance(this)!! }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        //initVariables
        attachGlobalListeners()

        //init ui
        binding.run {
            tvActivityName.text = this@VarActivity2::class.java.simpleName.toString()
            btCheckLocal.setOnClickListener { tvTerminalValueOnDemand.flash(getAllVariablesStr(cleverTapAPI)) }
            btParse.setOnClickListener { parseVariables() }
            btSync.setOnClickListener { syncVariables() }
            btDefineVars.setOnClickListener { defineVariablesAndAttachVariableListeners() }
            btNextActivity.setOnClickListener { }
            btLogVarCache.setOnClickListener { toast("loggin method removed") }
        }
    }

    private fun parseVariables(){
        toast("calling:parseVariables")
        cleverTapAPI.parseVariablesForClasses(Vars2::class.java)
        cleverTapAPI.parseVariables(Vars2.Vars2Instance.singleton())
    }
    private fun defineVariablesAndAttachVariableListeners(){
        toast("calling:defineVariables")
        Vars2.defineVarsWithListeners(cleverTapAPI)
    }

    private fun syncVariables(){
        toast("calling:syncVariables")
        cleverTapAPI.syncVariables()
    }


    private fun attachGlobalListeners() {
        cleverTapAPI.run {
            addVariablesChangedCallback(multiCallback)
            addOneTimeVariablesChangedCallback(oneTimeCallback)
        }
    }
    private val multiCallback = object : VariablesChangedCallback() { override fun variablesChanged() { binding.tvTerminalWithGlobalListenerMultiple.flash(getAllVariablesStr(cleverTapAPI)) } }
    private val oneTimeCallback = object : VariablesChangedCallback() { override fun variablesChanged() { binding.tvTerminalWithGlobalListenerOneTime.flash(getAllVariablesStr(cleverTapAPI)) } }


    override fun onDestroy() {
        super.onDestroy()
        cleverTapAPI.removeOneTimeVariablesChangedCallback(oneTimeCallback)
        cleverTapAPI.removeVariablesChangedCallback(multiCallback)
    }

}