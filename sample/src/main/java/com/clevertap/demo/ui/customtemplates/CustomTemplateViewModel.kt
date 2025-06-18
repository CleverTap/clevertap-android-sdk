package com.clevertap.demo.ui.customtemplates

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.util.Log
import com.clevertap.android.sdk.CleverTapAPI

data class CustomTemplateDialogState(
    val isVisible: Boolean = false,
    val config: OverlayDialogConfig? = null
)

class CustomTemplateViewModel(
    private val cleverTapInstance: CleverTapAPI?
) : ViewModel() {
    
    companion object {
        private const val TAG = "CustomTemplateViewModel"
    }
    
    // Dialog state
    var dialogState by mutableStateOf(CustomTemplateDialogState())
        private set
    
    /**
     * Show a custom template dialog with the provided configuration
     */
    fun showDialog(
        title: String,
        message: String,
        imageUrl: String? = null,
        primaryButtonText: String = "Continue",
        secondaryButtonText: String = "Close",
        onPrimaryAction: () -> Unit = {},
        onSecondaryAction: () -> Unit = {},
    ) {
        Log.d(TAG, "showDialog called with title: $title, message: $message")
        
        val config = OverlayDialogConfig(
            title = title,
            message = message,
            image = imageUrl,
            primaryButtonText = primaryButtonText,
            secondaryButtonText = secondaryButtonText,
            onPrimaryClick = {
                Log.d(TAG, "Primary button clicked")
                onPrimaryAction()
            },
            onSecondaryClick = {
                Log.d(TAG, "Secondary button clicked")
                onSecondaryAction()
            },
            onDismiss = {
                hideDialog()
            }
        )
        
        dialogState = CustomTemplateDialogState(
            isVisible = true,
            config = config
        )
    }
    
    /**
     * Hide the currently visible dialog
     */
    fun hideDialog() {
        dialogState = CustomTemplateDialogState(
            isVisible = false,
            config = null
        )
    }
    
    /**
     * Force hide dialog (useful for emergency cases)
     */
    fun forceHideDialog() {
        Log.w(TAG, "Force hiding dialog")
        dialogState = CustomTemplateDialogState(isVisible = false, config = null)
    }

    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared")
        // Force hide any active dialogs when ViewModel is cleared
        forceHideDialog()
    }
}
