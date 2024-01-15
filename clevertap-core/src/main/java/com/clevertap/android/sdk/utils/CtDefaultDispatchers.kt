package com.clevertap.android.sdk.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class CtDefaultDispatchers: DispatcherProvider {
    override fun io(): CoroutineDispatcher = Dispatchers.IO

    override fun main(): CoroutineDispatcher = Dispatchers.Main

    override fun processing(): CoroutineDispatcher = Dispatchers.Unconfined
}