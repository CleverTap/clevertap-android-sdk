package com.clevertap.demo.ui.main

import android.util.Log
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.variables.Var
import com.clevertap.android.sdk.variables.callbacks.VariableCallback

object FileVarsData {

    fun defineFileVars(
        cleverTapAPI: CleverTapAPI,
        tag: String = "FileVarsData"
    ) {
        val file1: Var<String>? = cleverTapAPI.defineFileVariable("folder1.fileVariable")
            ?.apply {
                addFileReadyHandler(object : VariableCallback<String>() {
                    override fun onValueChanged(variable: Var<String>?) {
                        Log.i(tag, "file1 ready: ${value()}")
                    }
                })
            }
        val file2 = cleverTapAPI?.defineFileVariable("folder1.folder2.fileVariable")
            ?.apply {
                addFileReadyHandler(object : VariableCallback<String>() {
                    override fun onValueChanged(variable: Var<String>?) {
                        Log.i(tag, "file2 ready: ${value()}")
                    }
                })
            }
        val file3 = cleverTapAPI?.defineFileVariable("folder1.folder3.fileVariable")
            ?.apply {
                addFileReadyHandler(object : VariableCallback<String>() {
                    override fun onValueChanged(variable: Var<String>?) {
                        Log.i(tag, "file3 ready: ${value()}")
                    }
                })
            }
    }
}