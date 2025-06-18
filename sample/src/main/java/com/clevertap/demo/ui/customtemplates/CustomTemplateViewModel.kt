package com.clevertap.demo.ui.customtemplates

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.util.Log
import com.clevertap.android.sdk.CleverTapAPI
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

data class CustomTemplateDialogState(
    val isVisible: Boolean = false,
    val config: OverlayDialogConfig? = null
)

class CustomTemplateViewModel() : ViewModel() {
    
    companion object {
        private const val TAG = "CustomTemplateViewModel"
    }
    
    // Dialog state
    var dialogState by mutableStateOf(CustomTemplateDialogState())
        private set
    
    // Coroutine scope for auto-close functionality
    private val viewModelScope = CoroutineScope(Dispatchers.Main)
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
        Log.d(TAG, "showDialog called with title: $title, message: $message, autoCloseAfter: ${autoCloseAfterSeconds}s")
        
        // Cancel any existing auto-close job
        autoCloseJob?.cancel()
        
        val config = OverlayDialogConfig(
            title = title,
            message = message,
            image = imageUrl,
            primaryButtonText = primaryButtonText,
            secondaryButtonText = secondaryButtonText,
            onPrimaryClick = {
                Log.d(TAG, "Primary button clicked")
                autoCloseJob?.cancel() // Cancel auto-close when user interacts
                onPrimaryAction()
            },
            onSecondaryClick = {
                Log.d(TAG, "Secondary button clicked")
                autoCloseJob?.cancel() // Cancel auto-close when user interacts
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
        
        // Setup auto-close if specified (value > 0)
        if (autoCloseAfterSeconds > 0) {
            autoCloseJob = viewModelScope.launch {
                delay(autoCloseAfterSeconds * 1000L) // Convert seconds to milliseconds
                onAutoClose()
            }
        }
    }
    
    /**
     * Hide the currently visible dialog
     */
    fun hideDialog() {
        Log.d(TAG, "Hiding dialog")
        // Cancel auto-close job when manually hiding dialog
        autoCloseJob?.cancel()
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
        autoCloseJob?.cancel()
        dialogState = CustomTemplateDialogState(isVisible = false, config = null)
    }

    
    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel cleared")
        autoCloseJob?.cancel()
        viewModelScope.cancel()
        forceHideDialog()
    }
}
