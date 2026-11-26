package com.clevertap.android.sdk.utils

import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineDispatcher

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface DispatcherProvider {

    fun io() : CoroutineDispatcher

    fun main(): CoroutineDispatcher

    fun processing(): CoroutineDispatcher
}