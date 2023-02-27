package com.clevertap.demo

import android.annotation.SuppressLint
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
@SuppressLint("SetTextI18n")
class TestActivity : AppCompatActivity() {
    var hasAttachedListenrs = false

    private var ctApi: CleverTapAPI? = null
    val binding: ActivityTestBinding by lazy { ActivityTestBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        FakeServer.ctx = this
        ctApi = CleverTapAPI.getDefaultInstance(this)

        with(binding) {
            btCheckLocalWithApiFailure.setOnClickListener(::checkLocalValuesAfterApiFailure)
            btCheckLocal.setOnClickListener(::checkLocalValues)
            btInit.setOnClickListener(this@TestActivity::init)
            btForceSync.setOnClickListener(::onForceSyncClick)
            btrequestFromServer.setOnClickListener(::wzrkFetch)
        }
    }

    private fun checkLocalValuesAfterApiFailure(view: View?) {
        //CTVariables.setVariableContext(this)// ActivityLifeCycleManager will do this. for assuming its done in Application#onCreate after super.onCreate()

        ctApi?.parseVariablesForClasses(TestMyVars::class.java) //user's  will do this in Application#onCreate before super.onCreate()
        attachListeners() //user's  will do this at any point of time. for now, assuming its done in Application#onCreate before super.onCreate()

        ctApi?.init()  // ActivityLifeCycleManager will do this. for assuming its done in Application#onCreate after super.onCreate()
        ctApi?.onAppLaunchFail() // BaseResponse#processResponse will do this once data is available
    }

    private fun attachListeners() {
        if(hasAttachedListenrs) return
        ctApi?.addVariablesChangedHandler(object : VariablesChangedCallback() {
            override fun variablesChanged() {
                binding.tvTerminalWithGlobalListenerMultiple.text = ("variablesChanged()\n${getVarsString()}")
                flashTextView(binding.tvTerminalWithGlobalListenerMultiple)
            }
        })
        ctApi?.addOneTimeVariablesChangedHandler(object : VariablesChangedCallback() {
            override fun variablesChanged() {
                binding.tvTerminalWithGlobalListenerOneTime.text = ("variablesChanged()\n${getVarsString()}")
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
        ctApi?.parseVariablesForClasses(TestMyVars::class.java) //user will do this in Application#onCreate before super.onCreate()
        attachListeners() //user's  will do this at any point of time. for now, assuming its done in Application#onCreate before super.onCreate()

        //CTVariables.setVariableContext(this)// ActivityLifeCycleManager will do this. for assuming its done in Application#onCreate after super.onCreate()
        ctApi?.init()  // ActivityLifeCycleManager will do this. for assuming its done in Application#onCreate after super.onCreate()

        fakeServerRequestForData(1) // similar to calling app launched  once sdk is iniitalised . its response via  BaseResponse#processResponse  will call CTVariables.handleVariableResponse(jsonObject)
    }

    private fun fakeServerRequestForData(jsonId: Int) {
        Log.v("TAG","requesting data from server")
        FakeServer.simulateGetVarsRequest(jsonId) { jsonObject: JSONObject? ->
            ctApi?.onAppLaunchSuccess(jsonObject)
        }
    }


    var toggle= true
    private fun wzrkFetch(view: View?) {
        toggle = !toggle
        fakeServerRequestForData(if(toggle)2 else 1)
        toast("requesting data from server...")
    }

    private fun onForceSyncClick(view: View?) {
        ctApi?.pushVariablesToServer()
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
            appendLine("response received=" + ctApi?.isVariableResponseReceived())
            appendLine("response received=" + CTVariables.isInDevelopmentMode())
            appendLine("- checked on : ${Date()}")
            appendLine("- DirectAccess:")
            appendLine("- welcomeMsg = ${TestMyVars.welcomeMsg}")
            appendLine("- isOptedForOffers = ${TestMyVars.isOptedForOffers}")
            appendLine("- initialCoins = ${TestMyVars.initialCoins}")
            appendLine("- correctGuessPercentage = ${TestMyVars.correctGuessPercentage}")
            appendLine("- userConfigurableProps = ${TestMyVars.userConfigurableProps}")
//            appendLine("- aiNames = ${TestMyVars.aiNames}")
            appendLine("- samsungS22 = ${TestMyVars.samsungS22}")
            appendLine("- samsungS23 = ${TestMyVars.samsungS23}")
            appendLine("- nokia12 = ${TestMyVars.nokia12}")
            appendLine("- nokia6a = ${TestMyVars.nokia6a}")
            appendLine("- appleI15 = ${TestMyVars.appleI15}")
            appendLine("-------------------------------------------------------------------")
            appendLine("Access via cache:")
            appendLine("welcomeMsg = ${ctApi?.getVariable<String>("welcomeMsg")}")
            appendLine("isOptedForOffers = ${ctApi?.getVariable<Boolean>("isOptedForOffers")}")
            appendLine("initialCoins = ${ctApi?.getVariable<Int>("initialCoins")}")
            appendLine("correctGuessPercentage = ${ctApi?.getVariable<Float>("correctGuessPercentage")}")
            appendLine("userConfigurableProps = ${ctApi?.getVariable<HashMap<String, Any>>("userConfigurableProps")}")
            appendLine("aiNames = ${ctApi?.getVariable<ArrayList<String>>("aiNames")}")
            appendLine("userConfigurableProps = ${ctApi?.getVariable<String>("userConfigurableProps")}")
            appendLine("android.samsung.s22 = ${ctApi?.getVariable<Double>("android.samsung.s22")}")
            appendLine("android.samsung.s23 = ${ctApi?.getVariable<String>("android.samsung.s23")}")
            appendLine("android.nokia.6a = ${ctApi?.getVariable<Double>("android.nokia.6a")}")
            appendLine("android.nokia.12 = ${ctApi?.getVariable<String>("android.nokia.12")}")
            appendLine("apple.iphone15 = ${ctApi?.getVariable<String>("apple.iphone15")}")

            appendLine("group:android = ${ctApi?.getVariable<Any>("android")}")
            appendLine("group:apple = ${ctApi?.getVariable<Any>("apple")}")
            this.toString()
        }
    }





}
