package com.clevertap.demo.ui.customtemplates

import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

data class OpenUrlConfirmDialogState(
    val isVisible: Boolean = false,
    val url: String = "",
    val onConfirm: () -> Unit = {},
    val onCancel: () -> Unit = {}
)

class OpenUrlConfirmViewModel : ViewModel() {
    
    // Dialog state
    var dialogState by mutableStateOf(OpenUrlConfirmDialogState())
        private set
    
    /**
     * Show the URL confirmation dialog
     */
    fun showDialog(
        url: String,
        onConfirm: () -> Unit = {},
        onCancel: () -> Unit = {}
    ) {

        dialogState = OpenUrlConfirmDialogState(
            isVisible = true,
            url = url,
            onConfirm = {
                onConfirm()
                hideDialog()
            },
            onCancel = {
                onCancel()
                hideDialog()
            }
        )
    }

    /**
     * Hide the dialog
     */
    fun hideDialog() {
        dialogState = OpenUrlConfirmDialogState(
            isVisible = false,
            url = "",
            onConfirm = {},
            onCancel = {}
        )
    }
    
    /**
     * Force hide dialog (cleanup)
     */
    fun forceHideDialog() {
        dialogState = OpenUrlConfirmDialogState(isVisible = false, url = "", onConfirm = {}, onCancel = {})
    }

    override fun onCleared() {
        super.onCleared()
        forceHideDialog()
    }
}
