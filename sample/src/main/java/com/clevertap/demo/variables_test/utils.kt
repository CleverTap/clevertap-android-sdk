package com.clevertap.demo.variables_test

import android.app.Activity
import android.graphics.Color
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.variables.CTVariables
import com.clevertap.demo.variables_test.models.Vars1
import com.clevertap.demo.variables_test.models.Vars2
import java.util.*
import kotlin.concurrent.thread


fun logg(msg:String){
    logg("",msg,null)
}

fun logg(start:String,msg:String,throwable: Throwable?){
    Log.e("ctv_UI||",start+ msg, throwable)
}
fun Activity.log(str:String, throwable: Throwable?=null){
    logg(this::class.java.name.toString()+"||",str, throwable)
}

fun Activity.toast(str:String){
    Toast.makeText(this, str, Toast.LENGTH_LONG).show()
    log(str)
}


fun TextView.flash(content:String?=null) {
    if(content!=null) this.text = content
    setBackgroundColor(Color.YELLOW)
    thread {
        Thread.sleep(300)
        (context as? Activity)?.runOnUiThread { setBackgroundColor(Color.WHITE) }
    }
}

fun getAllVariablesStr(cleverTapAPI:CleverTapAPI):String{
    val str = StringBuilder()
        .appendLine("isDevelopmentMode:${CTVariables.isInDevelopmentMode()} |checked on : ${Date()} " )
        .appendLine("--------------------------------")
        .appendLine("VarActivity1=========================:")
        .appendLine("(Defined----------------)")
        .appendLine(Vars1.getDefinedVars1(cleverTapAPI))
        .appendLine("(Parsed----------------)")
        .appendLine(Vars1.getParsedVars1(cleverTapAPI))

        .appendLine("VarActivity2=========================:")
        .appendLine("(Defined----------------)")
        .appendLine(Vars2.getDefinedVars2(cleverTapAPI))
        .appendLine("(Parsed----------------)")
        .appendLine(Vars2.getParsedVars2(cleverTapAPI))




    return str.toString()
}