package com.clevertap.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.demo.ui.main.HomeScreenViewModel
import com.clevertap.demo.ui.customtemplates.CustomTemplateViewModel
import com.clevertap.demo.ui.customtemplates.OpenUrlConfirmViewModel

class ViewModelFactory(
    private val cleverTapAPI: CleverTapAPI?,
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>) =
        with(modelClass) {
            when {
                isAssignableFrom(HomeScreenViewModel::class.java) ->
                    HomeScreenViewModel(cleverTapAPI)
                isAssignableFrom(CustomTemplateViewModel::class.java) ->
                    CustomTemplateViewModel()
                isAssignableFrom(OpenUrlConfirmViewModel::class.java) ->
                    OpenUrlConfirmViewModel()
                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T
}