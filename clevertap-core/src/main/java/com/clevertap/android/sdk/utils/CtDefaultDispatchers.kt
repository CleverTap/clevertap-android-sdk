package com.clevertap.android.sdk.utils

import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class CtDefaultDispatchers: DispatcherProvider {
    @Suppress("InjectDispatcher") // this class is the dispatcher injection point
    override fun io(): CoroutineDispatcher = Dispatchers.IO

    override fun main(): CoroutineDispatcher = Dispatchers.Main

    @Suppress("InjectDispatcher") // this class is the dispatcher injection point
    override fun processing(): CoroutineDispatcher = Dispatchers.Unconfined
}
