package com.clevertap.demo.ui.customtemplates

import com.clevertap.demo.MyApplication
import com.clevertap.demo.ViewModelFactory

/**
 * Singleton access point for CustomTemplateViewModel when viewModels() delegate cannot be used
 * (e.g., in CleverTap template presenters)
 */
object CustomTemplateManager {
    
    private var viewModelInstance: CustomTemplateViewModel? = null
    
    private val viewModelFactory by lazy {
        ViewModelFactory(MyApplication.ctInstance)
    }
    
    /**
     * Get or create the singleton ViewModel instance using the main ViewModelFactory
     */
    fun getViewModel(): CustomTemplateViewModel {
        return viewModelInstance ?: synchronized(this) {
            viewModelInstance ?: viewModelFactory.create(CustomTemplateViewModel::class.java).also {
                viewModelInstance = it
            }
        }
    }

    /**
     * Set the ViewModel instance (called from Activity when using proper lifecycle)
     */
    fun setViewModel(viewModel: CustomTemplateViewModel) {
        synchronized(this) {
            viewModelInstance = viewModel
        }
    }

}
