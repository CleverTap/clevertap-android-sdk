package com.clevertap.demo.ui.main

import android.util.Log
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext.TemplateContext
import com.clevertap.android.sdk.inapp.customtemplates.FunctionPresenter
import com.clevertap.android.sdk.inapp.customtemplates.TemplatePresenter
import com.clevertap.android.sdk.inapp.customtemplates.function
import com.clevertap.android.sdk.inapp.customtemplates.template
import com.clevertap.android.sdk.inapp.customtemplates.templatesSet

object CustomInAppTemplates {
    private const val TAG = "CustomInAppTemplates"

    private val templatePresenter = object : TemplatePresenter {
        override fun onClose(context: TemplateContext) {
            Log.i(TAG, "Template closed ${context.templateName}")
            context.setDismissed()
        }

        override fun onPresent(context: TemplateContext) {
            Log.i(TAG, "Template presented = ${context.templateName}")
            Log.i(TAG, "Template string argument = ${context.getString("var2")}")
            Log.i(TAG, "Template double argument = ${context.getDouble("folder1.var3")}")
            Log.i(TAG, "Template boolean argument = ${context.getBoolean("var1")}")
            Log.i(TAG, "Template toString = $context")
//        context.triggerActionArgument("actionArg", this)
            context.setPresented()
        }
    }

    private val functionPresenter = FunctionPresenter { context ->
        Log.i(TAG, "Function presented ${context.templateName}")
        Log.i(TAG, "Function string argument = ${context.getString("height")}")
        Log.i(TAG, "Function double argument = ${context.getDouble("newvar3")}")
        Log.i(TAG, "Function boolean argument = ${context.getBoolean("a")}")
        Log.i(TAG, "Function toString = $context")
        context.setPresented()
    }

    fun registerCustomTemplates() =
        templatesSet(
            template {
                name("template-a")
                booleanArgument("var1", true)
                stringArgument("var2", "text")
                doubleArgument("folder1.var3", 3.14)
                actionArgument("folder1.var4")
                presenter(templatePresenter)
            },
            function(isVisual = false) {
                name("function-a")
                stringArgument("newvar1", "text")
                booleanArgument("newvar2", true)
                booleanArgument("newvar4", true)
                doubleArgument("newvar3", 3.14)
                presenter(functionPresenter)
            },
            template {
                name("template-c")
                booleanArgument("bool", false)
                stringArgument("string", "Default")
                actionArgument("action")
                mapArgument(
                    "map", mapOf(
                        "string" to "Default",
                        "int" to 0
                    )
                )
                presenter(templatePresenter)
            },
            function(isVisual = true) {
                name("function-lalit")
                stringArgument("height", "999")
                booleanArgument("a", true)
                doubleArgument("newvar3", 5.14)
                presenter(functionPresenter)
            },
            function(isVisual = false) {
                name("function-visual.false")
                stringArgument("Thursday", "999")
                booleanArgument("email", true)
                booleanArgument("phone", true)
                fileArgument("pansies3.jpg")
                presenter(functionPresenter)
            }
        )
}