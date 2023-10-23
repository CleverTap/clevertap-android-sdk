package com.clevertap.android.sdk

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.inapp.ImpressionStore
import com.clevertap.android.sdk.inapp.InAppStore

class StoreProvider {

    companion object {

        @Volatile
        private var INSTANCE: StoreProvider? = null

        @JvmStatic
        fun getInstance(): StoreProvider =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: StoreProvider()
                    .also { INSTANCE = it }
            }
    }

    fun provideInAppStore(
        context: Context,
        cryptHandler: CryptHandler,
        deviceInfo: DeviceInfo,
        accountId: String
    ): InAppStore {
        val prefName = "${Constants.INAPP_KEY}:${deviceInfo.deviceID}:$accountId"
        return InAppStore(getCTPreference(context, prefName), cryptHandler)
    }

    fun provideImpressionStore(
        context: Context,
        deviceInfo: DeviceInfo,
        accountId: String
    ): ImpressionStore {
        val prefName = "${Constants.KEY_COUNTS_PER_INAPP}:${deviceInfo.deviceID}:$accountId"
        return ImpressionStore(getCTPreference(context, prefName))
    }

    fun provideLegacyInAppStore(context: Context, config: CleverTapInstanceConfig): LegacyInAppStore {
        val prefName = Constants.CLEVERTAP_STORAGE_TAG
        return LegacyInAppStore(getCTPreference(context, prefName), config)
    }

    @VisibleForTesting
    fun getCTPreference(context: Context, prefName: String) = CTPreference(context, prefName)
}

