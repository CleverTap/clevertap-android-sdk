package com.clevertap.demo.ui.customtemplates

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CustomTemplateDialogState(
    val isVisible: Boolean = false,
    val config: OverlayDialogConfig? = null
)

class CustomTemplateViewModel() : ViewModel() {
    
    // Dialog state
    var dialogState by mutableStateOf(CustomTemplateDialogState())
        private set
    
    private var autoCloseJob: Job? = null
    
    /**
     * Show a custom template dialog with the provided configuration
     */
    fun showDialog(
        title: String,
        message: String,
        imageUrl: String? = null,
        primaryButtonText: String = "Continue",
        secondaryButtonText: String = "Close",
        autoCloseAfterSeconds: Int = 0,
        onPrimaryAction: () -> Unit = {},
        onSecondaryAction: () -> Unit = {},
        onAutoClose: () -> Unit = {},
    ) {
        // Cancel any existing auto-close job
        autoCloseJob?.cancel()
        
        val config = OverlayDialogConfig(
            title = title,
            message = message,
            image = imageUrl,
            primaryButtonText = primaryButtonText,
            secondaryButtonText = secondaryButtonText,
            onPrimaryClick = {
                onPrimaryAction()
                hideDialog()
            },
            onSecondaryClick = {
                onSecondaryAction()
                hideDialog()
            },
            onDismiss = {
                hideDialog()
            }
        )
        
        dialogState = CustomTemplateDialogState(
            isVisible = true,
            config = config
        )
        
        // Setup auto-close if specified (value > 0)
        if (autoCloseAfterSeconds > 0) {
            autoCloseJob = viewModelScope.launch {
                delay(autoCloseAfterSeconds * 1000L) // Convert seconds to milliseconds
                onAutoClose()
            }
        }
    }

    fun hideDialog() {
        // Cancel auto-close job when manually hiding dialog
        autoCloseJob?.cancel()
        dialogState = CustomTemplateDialogState(
            isVisible = false,
            config = null
        )
    }
    

    fun forceHideDialog() {
        autoCloseJob?.cancel()
        dialogState = CustomTemplateDialogState(isVisible = false, config = null)
    }

    
    override fun onCleared() {
        super.onCleared()
        autoCloseJob?.cancel()
        forceHideDialog()
    }
}
