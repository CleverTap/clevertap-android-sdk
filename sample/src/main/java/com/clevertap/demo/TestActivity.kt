package com.clevertap.demo

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.clevertap.android.sdk.feat_variable.CTVariables
import com.clevertap.android.sdk.feat_variable.FakeServer
import com.clevertap.android.sdk.feat_variable.Parser
import com.clevertap.android.sdk.feat_variable.VarCache
import com.clevertap.android.sdk.feat_variable.callbacks.VariablesChangedCallback
import com.clevertap.demo.databinding.ActivityTestBinding
import java.util.Date
import kotlin.concurrent.thread
import kotlin.math.log



//todo
// issue 1: on successful api request map not changed, istead set to oldvalue/null on variable access, but in VarCache(), it is correctly set
// issue 2: on successful api request list not changed, istead set to oldvalue/null on successful api request ,  but in VarCache(), it is correctly set
//observation  on successful api request, nested variables are set correctly via variable access, but not available via VarCache.getVariable("name") . guessing they are only available via VarCache.getVariable("groupx.groupy.name") instead , probably because ...
// missed case hadStarted issue in vars api
// missed case : var handlers

class TestActivity : AppCompatActivity() {
    val binding :ActivityTestBinding by lazy { ActivityTestBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        with(binding){
            btCheckLocalWithApiFailure.setOnClickListener(::checkLocalValuesAfterApiFailure)
            btCheckLocal.setOnClickListener(::checkLocalValues)
            btInit.setOnClickListener(::onAppLaunchClick)
            btForceSync.setOnClickListener(::onForceSyncClick)
            btrequestFromServer.setOnClickListener(::onForceRequestFromServer)
        }
    }

    private fun checkLocalValuesAfterApiFailure(view: View?) {
        CTVariables.removeAllVariablesChangedHandler()
        CTVariables.removeAllOneTimeVariablesChangedHandler()
        CTVariables.addVariablesChangedHandler(object : VariablesChangedCallback(){
            override fun variablesChanged() {
                binding.tvTerminalWithGlobalListenerMultiple.text = ("CALLED BY <multi,failure> variablesChanged()\n${getVarsString()}")
                flashTextView(binding.tvTerminalWithGlobalListenerMultiple)
            }
        })
        CTVariables.addOneTimeVariablesChangedHandler( object :VariablesChangedCallback(){
            override fun variablesChanged() {
                binding.tvTerminalWithGlobalListenerOneTime.text = ("CALLED BY <onetime,failure> variablesChanged()\n${getVarsString()}")
                flashTextView(binding.tvTerminalWithGlobalListenerOneTime)
            }
        })

        // user will do this before app.onCreate
        CTVariables.setContext(this)
        Parser.parseVariablesForClasses(TestMyVars::class.java)

        // user will do this after app.onCreate or in activity.onCreate
        CTVariables.initWithApiFailure()
    }

    private fun checkLocalValues(view: View?) {
        binding.tvTerminalValueOnDemand.text = getVarsString()
        flashTextView(binding.tvTerminalValueOnDemand)
    }

    private fun onAppLaunchClick(view: View?) {
        // user will do this before app.onCreate
        //CTVariables.setContext(this)
        //Parser.parseVariablesForClasses(TestMyVars::class.java)

        // user will do this after app.onCreate or in activity.onCreate

        CTVariables.removeAllVariablesChangedHandler()
        CTVariables.removeAllOneTimeVariablesChangedHandler()
        CTVariables.addVariablesChangedHandler(object : VariablesChangedCallback(){
            override fun variablesChanged() {
                binding.tvTerminalWithGlobalListenerMultiple.text = ("CALLED BY <multi> variablesChanged()\n${getVarsString()}")
                flashTextView(binding.tvTerminalWithGlobalListenerMultiple)
            }
        })
        CTVariables.addOneTimeVariablesChangedHandler( object :VariablesChangedCallback(){
            override fun variablesChanged() {
                binding.tvTerminalWithGlobalListenerOneTime.text = ("CALLED BY <onetime> variablesChanged()\n${getVarsString()}")
                flashTextView(binding.tvTerminalWithGlobalListenerOneTime)
            }
        })

        // user will do this before app.onCreate
        CTVariables.setContext(this)
        Parser.parseVariablesForClasses(TestMyVars::class.java)

        // user will do this after app.onCreate or in activity.onCreate
        CTVariables.init()
    }


    var toggle= true
    private fun onForceRequestFromServer(view: View?) {
        FakeServer.expectedBackendData = if(toggle)FakeServer.Companion.ResposnseType.VARS2_VARSCODE else FakeServer.Companion.ResposnseType.VARS1_VARSCODE
        toggle = !toggle
        CTVariables.requestVariableDataFromServer()
        toast("requesting data from server...")

    }


    private fun onForceSyncClick(view: View?) {
        FakeServer.localVarsJson = TestMyVars.getUpdatedJSon()
        CTVariables.pushVariablesToServer{}
    }

    fun toast(str:String){
        Toast.makeText(this, str, Toast.LENGTH_LONG).show()
    }

    private fun flashTextView(tv: TextView) {
        tv.setBackgroundColor(Color.YELLOW)
        thread {
            Thread.sleep(300)
            runOnUiThread {
                tv.setBackgroundColor(Color.WHITE)
            }
        }
    }

    private fun getVarsString():String {
        return StringBuilder().run {
            appendLine("- checked on : ${Date()}")
            appendLine("- DirectAccess:")
            appendLine("- welcomeMsg = ${TestMyVars.welcomeMsg}")
            appendLine("- isOptedForOffers = ${TestMyVars.isOptedForOffers}")
            appendLine("- initialCoins = ${TestMyVars.initialCoins}")
            appendLine("- correctGuessPercentage = ${TestMyVars.correctGuessPercentage}")
            appendLine("- userConfigurableProps = ${TestMyVars.userConfigurableProps}")
            appendLine("- aiNames = ${TestMyVars.aiNames}")
            appendLine("- samsungS22 = ${TestMyVars.samsungS22}")
            appendLine("- samsungS23 = ${TestMyVars.samsungS23}")
            appendLine("- nokia12 = ${TestMyVars.nokia12}")
            appendLine("- nokia6a = ${TestMyVars.nokia6a}")
            appendLine("- appleI15 = ${TestMyVars.appleI15}")
            appendLine("-------------------------------------------------------------------")
            appendLine("Access via cache:")
            appendLine("welcomeMsg = ${VarCache.getVariable<String>("welcomeMsg")}")
            appendLine("isOptedForOffers = ${VarCache.getVariable<Boolean>("isOptedForOffers")}")
            appendLine("initialCoins = ${VarCache.getVariable<Int>("initialCoins")}")
            appendLine("correctGuessPercentage = ${VarCache.getVariable<Float>("correctGuessPercentage")}")
            appendLine("userConfigurableProps = ${VarCache.getVariable<HashMap<String,Any>>("userConfigurableProps")}")
            appendLine("aiNames = ${VarCache.getVariable<ArrayList<String>>("aiNames")}")
            appendLine("userConfigurableProps = ${VarCache.getVariable<String>("userConfigurableProps")}")
            appendLine("android.samsung.s22 = ${VarCache.getVariable<Double>("android.samsung.s22")}")
            appendLine("android.samsung.s23 = ${VarCache.getVariable<String>("android.samsung.s23")}")
            appendLine("android.nokia.6a = ${VarCache.getVariable<Double>("android.nokia.6a")}")
            appendLine("android.nokia.12 = ${VarCache.getVariable<String>("android.nokia.12")}")
            appendLine("apple.iphone15 = ${VarCache.getVariable<String>("apple.iphone15")}")

            appendLine("group:android = ${VarCache.getVariable<Any>("android")}")
            appendLine("group:apple = ${VarCache.getVariable<Any>("apple")}")
            this.toString()
        }
    }





}
