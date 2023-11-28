package com.clevertap.android.sdk

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore
import com.clevertap.android.sdk.inapp.store.preference.InAppStore
import com.clevertap.android.sdk.inapp.store.preference.LegacyInAppStore
import com.clevertap.android.sdk.store.preference.CTPreference

const val STORE_TYPE_INAPP = 1
const val STORE_TYPE_IMPRESSION = 2
const val STORE_TYPE_LEGACY_INAPP = 3
const val STORE_TYPE_INAPP_ASSETS = 4

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

    fun provideInAppAssetsStore(
        context: Context,
        deviceInfo: DeviceInfo,
        accountId: String
    ): InAppAssetsStore {
        val prefName = constructStorePreferenceName(STORE_TYPE_INAPP_ASSETS, deviceInfo.deviceID, accountId)
        return InAppAssetsStore(getCTPreference(context, prefName))
    }

    fun provideInAppStore(
        context: Context,
        cryptHandler: CryptHandler,
        deviceInfo: DeviceInfo,
        accountId: String
    ): InAppStore {
        val prefName = constructStorePreferenceName(STORE_TYPE_INAPP, deviceInfo.deviceID, accountId)
        return InAppStore(getCTPreference(context, prefName), cryptHandler)
    }

    fun provideImpressionStore(
        context: Context,
        deviceInfo: DeviceInfo,
        accountId: String
    ): ImpressionStore {
        val prefName = constructStorePreferenceName(STORE_TYPE_IMPRESSION, deviceInfo.deviceID, accountId)
        return ImpressionStore(getCTPreference(context, prefName))
    }

    fun provideLegacyInAppStore(context: Context, accountId: String): LegacyInAppStore {
        val prefName = constructStorePreferenceName(STORE_TYPE_LEGACY_INAPP)
        return LegacyInAppStore(getCTPreference(context, prefName), accountId)
    }

    @VisibleForTesting
    fun getCTPreference(context: Context, prefName: String) = CTPreference(context, prefName)

    fun constructStorePreferenceName(storeType: Int, deviceId: String = "", accountId: String = ""): String =
        when (storeType) {
            STORE_TYPE_INAPP_ASSETS -> "inapp_assets:$deviceId:$accountId"
            STORE_TYPE_INAPP -> "${Constants.INAPP_KEY}:$deviceId:$accountId"
            STORE_TYPE_IMPRESSION -> "${Constants.KEY_COUNTS_PER_INAPP}:$deviceId:$accountId"
            STORE_TYPE_LEGACY_INAPP -> Constants.CLEVERTAP_STORAGE_TAG
            else -> Constants.CLEVERTAP_STORAGE_TAG
        }
}

