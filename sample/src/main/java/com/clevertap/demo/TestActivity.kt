package com.clevertap.demo

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.clevertap.android.sdk.*
import com.clevertap.android.sdk.variables.CTVariables
import com.clevertap.android.sdk.variables.FakeServer
import com.clevertap.android.sdk.variables.Parser
import com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback
import com.clevertap.demo.databinding.ActivityTestBinding
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.thread


//todo
// issue 1: on successful api request map not changed, istead set to oldvalue/null on variable access, but in VarCache(), it is correctly set
// issue 2: on successful api request list not changed, istead set to oldvalue/null on successful api request ,  but in VarCache(), it is correctly set
//observation  on successful api request, nested variables are set correctly via variable access, but not available via VarCache.getVariable("name") . guessing they are only available via VarCache.getVariable("groupx.groupy.name") instead , probably because ...
// missed case hadStarted issue in vars api
// missed case : var handlers

class TestActivity : AppCompatActivity() {
    val binding :ActivityTestBinding by lazy { ActivityTestBinding.inflate(layoutInflater) }

    private lateinit var ct:CleverTapAPI;
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
        ct = CleverTapAPI.getDefaultInstance(this)!!
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
        CTVariables.setVariableContext(this)
        Parser.parseVariablesForClasses(TestMyVars::class.java)

        // user will do this after app.onCreate or in activity.onCreate
        CTVariables.init()
        CTVariables.handleVariableResponse(null)
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

        Parser.parseVariablesForClasses(TestMyVars::class.java)
        ActivityLifecycleCallback.register(this.application)

        // user will do this before app.onCreate
        CTVariables.setVariableContext(this)


        // user will do this after app.onCreate or in activity.onCreate
        CTVariables.init()

        //---- remove once done ---
        fakeServerRequestForData()


    }

    private fun fakeServerRequestForData() {
        Logger.v("requesting data from server")
        FakeServer.simulateBERequest { jsonObject: JSONObject? ->
            CTVariables.handleVariableResponse(jsonObject)
        }
    }


    var toggle= true
    private fun onForceRequestFromServer(view: View?) {
        FakeServer.expectedBackendData = if(toggle)FakeServer.Companion.ResposnseType.VARS2_VARSCODE else FakeServer.Companion.ResposnseType.VARS1_VARSCODE
        toggle = !toggle
        fakeServerRequestForData()
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
            appendLine("response received="+ CTVariables.isVariableResponseReceived())
            appendLine("response received="+ CTVariables.isInDevelopmentMode())
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
            appendLine("welcomeMsg = ${CTVariables.getVariable<String>("welcomeMsg")}")
            appendLine("isOptedForOffers = ${CTVariables.getVariable<Boolean>("isOptedForOffers")}")
            appendLine("initialCoins = ${CTVariables.getVariable<Int>("initialCoins")}")
            appendLine("correctGuessPercentage = ${CTVariables.getVariable<Float>("correctGuessPercentage")}")
            appendLine("userConfigurableProps = ${CTVariables.getVariable<HashMap<String,Any>>("userConfigurableProps")}")
            appendLine("aiNames = ${CTVariables.getVariable<ArrayList<String>>("aiNames")}")
            appendLine("userConfigurableProps = ${CTVariables.getVariable<String>("userConfigurableProps")}")
            appendLine("android.samsung.s22 = ${CTVariables.getVariable<Double>("android.samsung.s22")}")
            appendLine("android.samsung.s23 = ${CTVariables.getVariable<String>("android.samsung.s23")}")
            appendLine("android.nokia.6a = ${CTVariables.getVariable<Double>("android.nokia.6a")}")
            appendLine("android.nokia.12 = ${CTVariables.getVariable<String>("android.nokia.12")}")
            appendLine("apple.iphone15 = ${CTVariables.getVariable<String>("apple.iphone15")}")

            appendLine("group:android = ${CTVariables.getVariable<Any>("android")}")
            appendLine("group:apple = ${CTVariables.getVariable<Any>("apple")}")
            this.toString()
        }
    }





}
