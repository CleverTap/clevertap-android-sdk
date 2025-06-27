package com.clevertap.demo.ui.customtemplates

import com.clevertap.demo.MyApplication
import com.clevertap.demo.ViewModelFactory

/**
 * Singleton access point for OpenUrlConfirmViewModel when viewModels() delegate cannot be used
 * (e.g., in CleverTap template presenters)
 */
object OpenUrlConfirmViewModelProvider {
    
    private var viewModelInstance: OpenUrlConfirmViewModel? = null
    
    private val viewModelFactory by lazy {
        ViewModelFactory(MyApplication.ctInstance)
    }
    
    /**
     * Get or create the singleton ViewModel instance using the main ViewModelFactory
     */
    fun getViewModel(): OpenUrlConfirmViewModel {
        return viewModelInstance ?: synchronized(this) {
            viewModelInstance ?: viewModelFactory.create(OpenUrlConfirmViewModel::class.java).also {
                viewModelInstance = it
            }
        }
    }

    /**
     * Set the ViewModel instance
     */
    fun setViewModel(viewModel: OpenUrlConfirmViewModel) {
        synchronized(this) {
            viewModelInstance = viewModel
        }
    }
}
