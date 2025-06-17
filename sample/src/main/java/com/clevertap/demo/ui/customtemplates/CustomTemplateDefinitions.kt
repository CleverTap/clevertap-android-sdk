package com.clevertap.demo.ui.customtemplates

import android.util.Log
import com.clevertap.android.sdk.inapp.customtemplates.TemplatePresenter
import com.clevertap.android.sdk.inapp.customtemplates.function
import com.clevertap.android.sdk.inapp.customtemplates.template
import com.clevertap.android.sdk.inapp.customtemplates.templatesSet
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext
import com.clevertap.android.sdk.inapp.customtemplates.FunctionPresenter

fun createCustomTemplates() = templatesSet(
    // Custom Interstitial Template (in-app)
    template {
        name("Custom Interstitial")
        stringArgument("Title", "Welcome!") // Title string with default
        stringArgument("Message", "This is a custom interstitial message that can be quite long and will be displayed in a scrollable view if needed. You can add multiple lines of text here and it will automatically scroll when the content exceeds the available space.") // Message string with default
        fileArgument("Image") // Image file (optional)
        actionArgument("Open action") // Action to trigger when button is pressed
        booleanArgument("Show close button", true) // Whether to show close button
        intArgument("Auto close after", 0) // Auto close timer in seconds (0 = no auto close)
        presenter(CustomInterstitialPresenter())
    },

    // Copy to Clipboard Function (non-visual)
    function(isVisual = false) {
        name("Copy to clipboard")
        stringArgument("Text", "Hello from CleverTap!") // Text string to copy with default
        presenter(CopyToClipboardPresenter())
    },

    // Open URL with Confirm Function (visual)
    function(isVisual = true) {
        name("Open URL with confirm")
        stringArgument("URL", "https://clevertap.com") // URL string to open with default
        presenter(OpenURLConfirmPresenter())
    }
)

class CustomInterstitialPresenter : TemplatePresenter {
    override fun onPresent(context: CustomTemplateContext.TemplateContext) {
        Log.d(
            "CustomTemplate",
            "onPresent called for template: ${context.templateName}"
        )
    }

    override fun onClose(context: CustomTemplateContext.TemplateContext) {
        Log.d(
            "CustomTemplate",
            "onClose called for template: ${context.templateName}"
        )
    }
}

class OpenURLConfirmPresenter : FunctionPresenter {
    override fun onPresent(context: CustomTemplateContext.FunctionContext) {
        Log.d(
            "CustomTemplate",
            "onPresent called for template: ${context.templateName}"
        )
    }
}

class CopyToClipboardPresenter : FunctionPresenter {
    override fun onPresent(context: CustomTemplateContext.FunctionContext) {
        Log.d(
            "CustomTemplate",
            "onPresent called for template: ${context.templateName}"
        )
    }
}