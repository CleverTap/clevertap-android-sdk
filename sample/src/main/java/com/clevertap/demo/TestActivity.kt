package com.clevertap.demo

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.task.CTExecutorFactory
import com.clevertap.android.sdk.variables.CTVariables
import com.clevertap.android.sdk.variables.Var
import com.clevertap.android.sdk.variables.callbacks.VariableCallback
import com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback
import com.clevertap.demo.databinding.ActivityTestBinding
import java.util.*
import kotlin.concurrent.thread

@SuppressLint("SetTextI18n")
class TestActivity : AppCompatActivity() {
    private var ctApi: CleverTapAPI? = null
    private val binding: ActivityTestBinding by lazy { ActivityTestBinding.inflate(layoutInflater) }

    private val javaInstance = TestMyVarsJavaInstance()
    private var toggle= false

    private var definedVar:Var<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        log("onCreate called")
        ctApi = CleverTapAPI.getDefaultInstance(this)

        parseVariables(ctApi!!).also { binding.btParse.isEnabled=false }
        defineVariables(ctApi!!).also { binding.btDefineVars.isEnabled=false }
        attachListeners(ctApi!!).also { binding.btAttachListeners.isEnabled=false }
        binding.btAppLaunch.isEnabled = false


        with(binding) {
            btCheckLocal.setOnClickListener{checkLocalValues()}
            btParse.setOnClickListener{parseVariables(ctApi!!)}
            btDefineVars.setOnClickListener{defineVariables(ctApi!!) }
            btAttachListeners.setOnClickListener { attachListeners(ctApi!!) }
            btAppLaunch.setOnClickListener { appLaunchRequest(ctApi!!) }
            btRequestWzrkFetch.setOnClickListener { wzrkFetchRequest(ctApi!!) }
            btServerReqFail.setOnClickListener { serverDataRequestFail(ctApi!!) }
            btSync.setOnClickListener { sync(ctApi!!) }

        }
    }

    override fun onResume() {
        appLaunchRequest(ctApi!!)
        super.onResume()
    }
    private fun checkLocalValues() {
        binding.tvTerminalValueOnDemand.text = "definedVars="+getDefinedVarsStr()+"\n========\n"+getParsedVarsString()
        flashTextView(binding.tvTerminalValueOnDemand)
    }

    private fun parseVariables(cleverTapAPI: CleverTapAPI){
        toast("parsing various classes")

        cleverTapAPI.parseVariablesForClasses(TestMyVarsJava::class.java)
        cleverTapAPI.parseVariables(javaInstance)
    }
    private fun defineVariables(cleverTapAPI: CleverTapAPI){
        toast("defining variables")
        definedVar = cleverTapAPI.defineVariable("definedVar","hello")
    }
    private fun attachListeners(cleverTapAPI: CleverTapAPI){
        toast("attaching various listeners")
        cleverTapAPI.addVariablesChangedHandler(object : VariablesChangedCallback() {
            override fun variablesChanged() {
                binding.tvTerminalWithGlobalListenerMultiple.text = ("variablesChanged()\n${getParsedVarsString()}")
                flashTextView(binding.tvTerminalWithGlobalListenerMultiple)
            }
        })
        cleverTapAPI.addOneTimeVariablesChangedHandler(object : VariablesChangedCallback() {
            override fun variablesChanged() {
                binding.tvTerminalWithGlobalListenerOneTime.text = ("variablesChanged()\n${getParsedVarsString()}")
                flashTextView(binding.tvTerminalWithGlobalListenerOneTime)
            }
        })

        log("define var is $definedVar")
        definedVar?.addValueChangedHandler(object :VariableCallback<String>(){
            override fun handle(variable: Var<String>?) {
                binding.tvTerminalWithIndividualListeners.text = (getDefinedVarsStr())
                flashTextView(binding.tvTerminalWithIndividualListeners)
            }

        })
    }


    //wzrk_fetch/ app_launch
    private fun appLaunchRequest(cleverTapAPI: CleverTapAPI){
        toast("requesting app launch")
        CTExecutorFactory
            .executors(cleverTapAPI.tempGetConfig())
            .postAsyncSafelyTask<Unit>()
            .execute("ctv_CleverTap#APP_LAUNCH(fake)") {
                val response = FakeServer.getJson(1, this)
                cleverTapAPI.tempGetVariablesApi().handleVariableResponse(response, null)
            }

    }

    private fun wzrkFetchRequest(cleverTapAPI: CleverTapAPI ){
        toast("requesting wzrk fetch")
        CTExecutorFactory
            .executors(cleverTapAPI.tempGetConfig())
            .postAsyncSafelyTask<Unit>()
            .execute("ctv_CleverTap#WZRK_FETCH(fake)") {
                val response =  FakeServer.getJson(if(toggle)1 else 2,this)
                cleverTapAPI.tempGetVariablesApi().handleVariableResponse(response){ log("handleVariableResponse:$it") }
                toggle!=toggle
            }
    }
    private fun wzrkFetchRequestActual(cleverTapAPI: CleverTapAPI ){
        toast("requesting wzrk fetch")
        cleverTapAPI.wzrkFetchVariables { log("handleVariableResponse:$it") }
        toggle!=toggle
    }


    //app launch fail
    private fun serverDataRequestFail(cleverTapAPI: CleverTapAPI){
        toast("requesting app launch(failed)")
        cleverTapAPI.tempGetVariablesApi().handleVariableResponse(null) { log("handleVariableResponse:$it") }

    }

    private fun sync(cleverTapAPI: CleverTapAPI){
        log("sync(pushVariablesToServer)")
        cleverTapAPI.pushVariablesToServer()
    }


    private fun getParsedVarsString():String {
        val ctApi = ctApi?:return  ""
        return StringBuilder().run {
            appendLine("isDevelopmentMode:${CTVariables.isInDevelopmentMode()} |checked on : ${Date()} " )

            appendLine("-------------------------------------------------------------------")

            appendLine("JavaStatic:")
            appendy("- welcomeMsg = ${TestMyVarsJava.welcomeMsg} | var:${ctApi.getVariable<String>("welcomeMsg")}")
            appendy("- isOptedForOffers = ${TestMyVarsJava.isOptedForOffers}| var:${ctApi.getVariable<Boolean>("isOptedForOffers")}")
            appendy("- initialCoins = ${TestMyVarsJava.initialCoins} | var:${ctApi.getVariable<Int>("initialCoins")}")
            appendy("- correctGuessPercentage = ${TestMyVarsJava.correctGuessPercentage}  | var:${ctApi.getVariable<Float>("correctGuessPercentage")}")
            appendy("- userConfigurableProps = ${TestMyVarsJava.userConfigurableProps}  | var:${ctApi.getVariable<HashMap<String, Any>>("userConfigurableProps")}")
            appendy("- samsungS22 = ${TestMyVarsJava.samsungS22}  | var:${ctApi.getVariable<Double>("android.samsung.s22")}")
            appendy("- samsungS23 = ${TestMyVarsJava.samsungS23}  | var:${ctApi.getVariable<String>("android.samsung.s23")}")
            appendy("- nokia12 = ${TestMyVarsJava.nokia12}  | var:${ctApi.getVariable<String>("android.nokia.12")}")
            appendy("- nokia6a = ${TestMyVarsJava.nokia6a}  | var:${ctApi.getVariable<Double>("android.nokia.6a")}")
            appendy("- appleI15 = ${TestMyVarsJava.appleI15}  | var:${ctApi.getVariable<String>("apple.iphone15")}")

            appendLine("-------------------------------------------------------------------")
            appendLine("JavaDynamic:")
            appendy("- javaIStr = ${javaInstance.javaIStr} | var:${ctApi.getVariable<String>("javaIStr")}")
            appendy("- javaIBool = ${javaInstance.javaIBool}| var:${ctApi.getVariable<Boolean>("javaIBool")}")
            appendy("- javaIInt = ${javaInstance.javaIInt} | var:${ctApi.getVariable<Int>("javaIInt")}")
            appendy("- javaIDouble = ${javaInstance.javaIDouble}  | var:${ctApi.getVariable<Double>("javaIDouble")}")

            this.toString()
        }
    }


    private fun getDefinedVarsStr(): String {
        return definedVar.toString()
    }

    fun StringBuilder.appendy(value: String?): StringBuilder = append(value).appendLine().appendLine()

    private fun toast(str:String){
        Toast.makeText(this, str, Toast.LENGTH_LONG).show()
        log(str)
    }
    private fun log(str:String,throwable: Throwable?=null){
        Log.e("ctv_TestActivity", str, throwable)
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
}
