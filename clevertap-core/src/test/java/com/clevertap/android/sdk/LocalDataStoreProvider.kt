package com.clevertap.android.sdk

import android.content.Context
import com.clevertap.android.sdk.cryption.CryptHandler

object LocalDataStoreProvider {

    fun provideLocalDataStore(
        context: Context,
        config: CleverTapInstanceConfig,
        cryptHandler: CryptHandler
    ): LocalDataStore {
        return LocalDataStore(context, config, cryptHandler)
    }
}
