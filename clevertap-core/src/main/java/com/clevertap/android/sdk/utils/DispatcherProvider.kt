package com.clevertap.android.sdk.utils

import kotlinx.coroutines.CoroutineDispatcher

interface DispatcherProvider {

    fun io() : CoroutineDispatcher

    fun main(): CoroutineDispatcher

    fun processing(): CoroutineDispatcher
}