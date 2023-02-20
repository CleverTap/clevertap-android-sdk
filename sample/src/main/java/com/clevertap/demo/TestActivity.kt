package com.clevertap.demo

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.variables.CTVariables
import com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback
import com.clevertap.demo.FakeServer.Companion.ResposnseType.VARS1_VARSCODE
import com.clevertap.demo.FakeServer.Companion.ResposnseType.VARS2_VARSCODE
import com.clevertap.demo.databinding.ActivityTestBinding
import org.json.JSONObject
import java.util.Date
import kotlin.concurrent.thread

//todo
// issue 1: on successful api request map not changed, istead set to oldvalue/null on variable access, but in VarCache(), it is correctly set
// issue 2: on successful api request list not changed, istead set to oldvalue/null on successful api request ,  but in VarCache(), it is correctly set
//observation  on successful api request, nested variables are set correctly via variable access, but not available via VarCache.getVariable("name") . guessing they are only available via VarCache.getVariable("groupx.groupy.name") instead , probably because ...
// missed case hadStarted issue in vars api
// missed case : var handlers

class TestActivity : AppCompatActivity() {

    private var defaultInstance: CleverTapAPI? = null
    val binding: ActivityTestBinding by lazy { ActivityTestBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        FakeServer.ctx = this
        defaultInstance = CleverTapAPI.getDefaultInstance(this)

        with(binding) {
            btCheckLocalWithApiFailure.setOnClickListener(::checkLocalValuesAfterApiFailure)
            btCheckLocal.setOnClickListener(::checkLocalValues)
            btInit.setOnClickListener(this@TestActivity::init)
            btForceSync.setOnClickListener(::onForceSyncClick)
            btrequestFromServer.setOnClickListener(::wzrkFetch)
        }
    }

    private fun checkLocalValuesAfterApiFailure(view: View?) {

        defaultInstance?.parseVariablesForClasses(TestMyVars::class.java) //user's  will do this in Application#onCreate before super.onCreate()
        attachListeners() //user's  will do this at any point of time. for now, assuming its done in Application#onCreate before super.onCreate()

        //CTVariables.setVariableContext(this)// ActivityLifeCycleManager will do this. for assuming its done in Application#onCreate after super.onCreate()
        defaultInstance?.init()  // ActivityLifeCycleManager will do this. for assuming its done in Application#onCreate after super.onCreate()
        defaultInstance?.onAppLaunchFail() // BaseResponse#processResponse will do this once data is available
    }

    var hasAttachedListenrs = false
    private fun attachListeners() {
        if(hasAttachedListenrs) return
        defaultInstance?.addVariablesChangedHandler(object : VariablesChangedCallback() {
            override fun variablesChanged() {
                binding.tvTerminalWithGlobalListenerMultiple.text =
                    ("CALLED BY <multi,failure> variablesChanged()\n${getVarsString()}")
                flashTextView(binding.tvTerminalWithGlobalListenerMultiple)
            }
        })
        defaultInstance?.addOneTimeVariablesChangedHandler(object : VariablesChangedCallback() {
            override fun variablesChanged() {
                binding.tvTerminalWithGlobalListenerOneTime.text =
                    ("CALLED BY <onetime,failure> variablesChanged()\n${getVarsString()}")
                flashTextView(binding.tvTerminalWithGlobalListenerOneTime)
            }
        })
        hasAttachedListenrs = true
    }

    private fun checkLocalValues(view: View?) {
        binding.tvTerminalValueOnDemand.text = getVarsString()
        flashTextView(binding.tvTerminalValueOnDemand)
    }

    private fun init(view: View?) {
        defaultInstance?.parseVariablesForClasses(TestMyVars::class.java) //user will do this in Application#onCreate before super.onCreate()
        attachListeners() //user's  will do this at any point of time. for now, assuming its done in Application#onCreate before super.onCreate()

        //CTVariables.setVariableContext(this)// ActivityLifeCycleManager will do this. for assuming its done in Application#onCreate after super.onCreate()
        defaultInstance?.init()  // ActivityLifeCycleManager will do this. for assuming its done in Application#onCreate after super.onCreate()
        fakeServerRequestForData() // similar to calling app launched  once sdk is iniitalised . its response via  BaseResponse#processResponse  will call CTVariables.handleVariableResponse(jsonObject)
    }

    private fun fakeServerRequestForData() {
        Log.v("TAG","requesting data from server")
        FakeServer.simulateBERequest { jsonObject: JSONObject? ->
            defaultInstance?.onAppLaunchSuccess(jsonObject)
        }
    }


    var toggle= true
    private fun wzrkFetch(view: View?) {
        FakeServer.expectedBackendData = if(toggle) VARS2_VARSCODE else VARS1_VARSCODE
        toggle = !toggle
        fakeServerRequestForData()
        toast("requesting data from server...")

    }


    private fun onForceSyncClick(view: View?) {
        defaultInstance?.pushVariablesToServer {}
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
            appendLine("response received=" + defaultInstance?.isVariableResponseReceived())
            appendLine("response received=" + CTVariables.isInDevelopmentMode())
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
            appendLine("welcomeMsg = ${defaultInstance?.getVariable<String>("welcomeMsg")}")
            appendLine("isOptedForOffers = ${defaultInstance?.getVariable<Boolean>("isOptedForOffers")}")
            appendLine("initialCoins = ${defaultInstance?.getVariable<Int>("initialCoins")}")
            appendLine("correctGuessPercentage = ${defaultInstance?.getVariable<Float>("correctGuessPercentage")}")
            appendLine("userConfigurableProps = ${defaultInstance?.getVariable<HashMap<String, Any>>("userConfigurableProps")}")
            appendLine("aiNames = ${defaultInstance?.getVariable<ArrayList<String>>("aiNames")}")
            appendLine("userConfigurableProps = ${defaultInstance?.getVariable<String>("userConfigurableProps")}")
            appendLine("android.samsung.s22 = ${defaultInstance?.getVariable<Double>("android.samsung.s22")}")
            appendLine("android.samsung.s23 = ${defaultInstance?.getVariable<String>("android.samsung.s23")}")
            appendLine("android.nokia.6a = ${defaultInstance?.getVariable<Double>("android.nokia.6a")}")
            appendLine("android.nokia.12 = ${defaultInstance?.getVariable<String>("android.nokia.12")}")
            appendLine("apple.iphone15 = ${defaultInstance?.getVariable<String>("apple.iphone15")}")

            appendLine("group:android = ${defaultInstance?.getVariable<Any>("android")}")
            appendLine("group:apple = ${defaultInstance?.getVariable<Any>("apple")}")
            this.toString()
        }
    }





}
