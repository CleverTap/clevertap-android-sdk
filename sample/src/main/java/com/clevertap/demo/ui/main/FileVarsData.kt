package com.clevertap.demo.ui.main

import android.util.Log
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.android.sdk.variables.Var
import com.clevertap.android.sdk.variables.callbacks.VariableCallback
import com.clevertap.android.sdk.variables.callbacks.VariablesChangedCallback

object FileVarsData {

    private val listFileVarNames = listOf(
        "fileVariableRoot",
        "folder1.fileVariable",
        "folder1.folder2.fileVariable",
        "folder1.folder3.fileVariable",
        "folder1.folder4.folder5.fileVariable",
        "assets.image.fileVariable",
        "assets.video.fileVariable",
        "assets.pdf.fileVariable",
        "assets.gif.fileVariable"
    )

    fun addGlobalCallbacks(
        cleverTapAPI: CleverTapAPI,
        tag: String = "FileVarsData",
        listenerCount: Int = 1
    ) {
        for (count in 0..listenerCount) {
            val l1 = object : VariablesChangedCallback() {
                override fun variablesChanged() {
                    Log.i(
                        tag,
                        "onVariablesChangedAndNoDownloadsPending from listener-$count - should come after each fetch"
                    )
                    printFileVariables(cleverTapAPI, tag)
                }
            }
            val l2 = object : VariablesChangedCallback() {
                override fun variablesChanged() {
                    Log.i(
                        tag,
                        "onceVariablesChangedAndNoDownloadsPending from listener-$count - should come only once globally"
                    )
                }
            }
            cleverTapAPI.onVariablesChangedAndNoDownloadsPending(l1)
            cleverTapAPI.onceVariablesChangedAndNoDownloadsPending(l2)
        }
    }

    fun defineFileVars(
        cleverTapAPI: CleverTapAPI,
        tag: String = "FileVarsData",
        fileReadyListenerCount: Int = 1
    ) {
        val list = mutableListOf<Var<String>>()
        val builder = StringBuilder()
        builder.append("File variables defined, current values:")
        builder.appendLine()
        listFileVarNames.forEach { name ->
            defineFileVarPlusListener(
                name = name,
                cleverTapAPI = cleverTapAPI,
                fileReadyListenerCount = fileReadyListenerCount,
                tag = tag
            )?.also { variable ->
                list.add(variable)
                builder.append(variable.name())
                builder.append(" : ")
                builder.append(variable.value())
                builder.appendLine()
            }
        }
        Log.i(tag, builder.toString())
    }

    private fun defineFileVarPlusListener(
        name: String,
        cleverTapAPI: CleverTapAPI,
        fileReadyListenerCount: Int,
        tag: String
    ): Var<String>? {
        return cleverTapAPI.defineFileVariable(name)
            ?.apply {
                for (count in 0..fileReadyListenerCount) {
                    addFileReadyHandler(object : VariableCallback<String>() {
                        override fun onValueChanged(variable: Var<String>?) {
                            Log.i(tag, "${variable?.name()} ready: ${value()} from listener $count")
                        }
                    })
                }
            }
    }

    fun printFileVariables(
        cleverTapAPI: CleverTapAPI,
        tag: String = "FileVarsData"
    ) {
        val builder = StringBuilder()
        builder.append("List of file variables:")
        builder.appendLine()
        listFileVarNames.forEach {  name ->
            val variable = cleverTapAPI.getVariable<String>(name)
            if (variable != null) {
                builder.append(variable.name())
                builder.append(" : ")
                builder.append(variable.value())
                builder.appendLine()
            }
        }
        Log.i(tag, builder.toString())
    }
}