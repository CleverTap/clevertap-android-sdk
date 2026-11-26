package com.clevertap.android.sdk.utils

import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CtDefaultDispatchers: DispatcherProvider {
    override fun io(): CoroutineDispatcher = Dispatchers.IO

    override fun main(): CoroutineDispatcher = Dispatchers.Main

    override fun processing(): CoroutineDispatcher = Dispatchers.Unconfined
}
