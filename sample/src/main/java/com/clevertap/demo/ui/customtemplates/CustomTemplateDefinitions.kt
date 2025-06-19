package com.clevertap.demo.ui.customtemplates

import android.content.ClipData
import android.content.ClipboardManager
import android.util.Log
import com.clevertap.android.sdk.inapp.customtemplates.TemplatePresenter
import com.clevertap.android.sdk.inapp.customtemplates.function
import com.clevertap.android.sdk.inapp.customtemplates.template
import com.clevertap.android.sdk.inapp.customtemplates.templatesSet
import com.clevertap.android.sdk.inapp.customtemplates.CustomTemplateContext
import com.clevertap.android.sdk.inapp.customtemplates.FunctionPresenter

fun createCustomTemplates(
    customInterPresenter: CustomInterstitialPresenter,
    copyToClipboardPresenter: CopyToClipboardPresenter
) = templatesSet(
    // Custom Interstitial Template (in-app)
    template {
        name("Custom Interstitial")
        stringArgument("Title", "Welcome!")
        stringArgument(
            "Message",
            "This is a custom interstitial message that can be quite long and will be displayed in a scrollable view if needed. You can add multiple lines of text here and it will automatically scroll when the content exceeds the available space."
        ) // Message string with default
        fileArgument("Image")
        actionArgument("Open action")
        booleanArgument("Show close button", true)
        intArgument(
            "Auto close after",
            0
        ) // Auto close dialog after specified seconds (0 = no auto close)
        presenter(customInterPresenter)
    },

    // Copy to Clipboard Function (non-visual)
    function(isVisual = false) {
        name("Copy to clipboard")
        stringArgument("Text", "Hello from CleverTap!") // Text string to copy with default
        presenter(copyToClipboardPresenter)
    },

    // Open URL with Confirm Function (visual)
    function(isVisual = true) {
        name("Open URL with confirm")
        stringArgument("URL", "https://clevertap.com") // URL string to open with default
        presenter(OpenURLConfirmPresenter())
    }
)

class CustomInterstitialPresenter() : TemplatePresenter {
    override fun onPresent(context: CustomTemplateContext.TemplateContext) {
        // Extract template data
        val title = context.getString("Title") ?: "Welcome!"
        val message = context.getString("Message") ?: "This is a custom message"
        val imageFile = context.getFile("Image")
        val showCloseButton = context.getBoolean("Show close button") ?: true
        val autoCloseAfter =
            context.getInt("Auto close after") ?: 0 // Get auto-close duration in seconds

        val viewModel = CustomTemplateManager.getViewModel()

        viewModel.showDialog(
            title = title,
            message = message,
            imageUrl = imageFile?.toString(),
            primaryButtonText = "Trigger",
            secondaryButtonText = if (showCloseButton) "Close" else "",
            autoCloseAfterSeconds = autoCloseAfter,
            onPrimaryAction = {
                context.triggerActionArgument("Open action")
                onClose(context)
            },
            onSecondaryAction = {
                onClose(context)
            },
            onAutoClose = {
                onClose(context)
            }
        )

        // Mark template as presented
        context.setPresented()
    }

    override fun onClose(context: CustomTemplateContext.TemplateContext) {
        val viewModel = CustomTemplateManager.getViewModel()
        viewModel.hideDialog()
        context.setDismissed()
    }
}

class OpenURLConfirmPresenter : FunctionPresenter {
    override fun onPresent(context: CustomTemplateContext.FunctionContext) {
        Log.d(
            "CustomTemplate",
            "onPresent called for template: ${context.templateName}"
        )

        context.setPresented()
    }
}

class CopyToClipboardPresenter(private val clipboardManager: ClipboardManager) : FunctionPresenter {
    override fun onPresent(context: CustomTemplateContext.FunctionContext) {
        val copiedData = context.getString("Text")
        clipboardManager.setPrimaryClip(
            ClipData.newPlainText("Text", copiedData)
        )
        context.setPresented()

        // non-visual functions need not be dismissed
        // context.setDismissed()
    }
}