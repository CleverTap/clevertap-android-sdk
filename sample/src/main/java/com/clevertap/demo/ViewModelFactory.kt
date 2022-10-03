package com.clevertap.demo

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.clevertap.android.sdk.CleverTapAPI
import com.clevertap.demo.ui.main.HomeScreenViewModel

class ViewModelFactory(
    private val cleverTapAPI: CleverTapAPI?,
    private val activity: FragmentActivity?
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>) =
        with(modelClass) {
            when {
                isAssignableFrom(HomeScreenViewModel::class.java) ->
                    HomeScreenViewModel(cleverTapAPI,activity)
                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T
}