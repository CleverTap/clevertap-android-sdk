package com.clevertap.demo.ui.customtemplates

import android.content.Context
import android.util.Log
import com.clevertap.android.sdk.inapp.customtemplates.TemplatePresenter
import com.clevertap.android.sdk.inapp.customtemplates.function
import com.clevertap.android.sdk.inapp.customtemplates.template
import com.clevertap.android.sdk.inapp.customtemplates.templatesSet
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext
import com.clevertap.android.sdk.inapp.customtemplates.FunctionPresenter
import com.clevertap.demo.HomeScreenActivity
import com.clevertap.demo.utils.ActivityTracker

fun createCustomTemplates(customInterPresenter: CustomInterstitialPresenter) = templatesSet(
    // Custom Interstitial Template (in-app)
    template {
        name("Custom Interstitial")
        stringArgument("Title", "Welcome!")
        stringArgument("Message", "This is a custom interstitial message that can be quite long and will be displayed in a scrollable view if needed. You can add multiple lines of text here and it will automatically scroll when the content exceeds the available space.") // Message string with default
        fileArgument("Image")
        actionArgument("Open action")
        booleanArgument("Show close button", true)
        intArgument("Auto close after", 0)
        presenter(customInterPresenter)
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

class CustomInterstitialPresenter(private val appCtx: Context) : TemplatePresenter {
    override fun onPresent(context: CustomTemplateContext.TemplateContext) {
        // Extract template data
        val title = context.getString("Title") ?: "Welcome!"
        val message = context.getString("Message") ?: "This is a custom message"
        val imageFile = context.getFile("Image")
        val showCloseButton = context.getBoolean("Show close button") ?: true
        val autoCloseAfter = context.getInt("Auto close after") ?: 0
        
        Log.d("CustomTemplate", "Title: $title")
        Log.d("CustomTemplate", "Message: $message")
        Log.d("CustomTemplate", "Show close button: $showCloseButton")
        Log.d("CustomTemplate", "Auto close after: $autoCloseAfter")
        Log.d("CustomTemplate", "Image file: ${imageFile?.let { "Available: $it" } ?: "Not provided"}")
        
        // Get the current activity from CleverTap context
        val activity = ActivityTracker.currentForegroundActivity
        if (activity is HomeScreenActivity) {
            // Show the overlay dialog with template data
            activity.showCustomTemplateDialog(
                title = title,
                message = message,
                imageUrl = imageFile?.toString(), // Convert file to string for URL
                primaryButtonText = "Continue",
                secondaryButtonText = if (showCloseButton) "Close" else "Cancel",
                onPrimaryAction = {
                   context.triggerActionArgument("Open action", activity)
                },
                onSecondaryAction = {
                    Log.d("CustomTemplate", "Secondary action - closing template")
                },
            )
            context.setPresented()
        } else {
            Log.e("CustomTemplate", "Activity is not HomeScreenActivity, cannot show dialog")
        }
    }

    override fun onClose(context: CustomTemplateContext.TemplateContext) {
        context.setDismissed()
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