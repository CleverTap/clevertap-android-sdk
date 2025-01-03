package com.clevertap.android.sdk

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.clevertap.android.sdk.StoreProvider.Companion.INSTANCE
import com.clevertap.android.sdk.cryption.CryptHandler
import com.clevertap.android.sdk.inapp.store.preference.FileStore
import com.clevertap.android.sdk.inapp.store.preference.ImpressionStore
import com.clevertap.android.sdk.inapp.store.preference.InAppAssetsStore
import com.clevertap.android.sdk.inapp.store.preference.InAppStore
import com.clevertap.android.sdk.inapp.store.preference.LegacyInAppStore
import com.clevertap.android.sdk.store.preference.CTPreference

const val STORE_TYPE_INAPP = 1
const val STORE_TYPE_IMPRESSION = 2
const val STORE_TYPE_LEGACY_INAPP = 3
const val STORE_TYPE_INAPP_ASSETS = 4
const val STORE_TYPE_FILES = 5

/**
 * The `StoreProvider` class is responsible for providing different types of stores
 * used in the CleverTap SDK, such as In-App Store, Impression Store, Legacy In-App Store,
 * and In-App Assets Store. It ensures that only one instance of the [StoreProvider]
 * is created and provides methods to obtain instances of various stores.
 *
 * @property STORE_TYPE_INAPP Represents the type code for the In-App Store.
 * @property STORE_TYPE_IMPRESSION Represents the type code for the Impression Store.
 * @property STORE_TYPE_LEGACY_INAPP Represents the type code for the Legacy In-App Store.
 * @property STORE_TYPE_INAPP_ASSETS Represents the type code for the In-App Assets Store.
 *
 * @property INSTANCE The singleton instance of the [StoreProvider].
 */
internal class StoreProvider {

    companion object {

        @Volatile
        private var INSTANCE: StoreProvider? = null

        private const val ASSET_STORE_PREFIX = "inapp_assets"
        private const val FILE_STORE_PREFIX = "ct_files"

        /**
         * Gets the singleton instance of the [StoreProvider].
         *
         * @return The [StoreProvider] instance.
         */
        @JvmStatic
        fun getInstance(): StoreProvider =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: StoreProvider()
                    .also { INSTANCE = it }
            }
    }

    /**
     * Provides an instance of [InAppAssetsStore] using the given parameters.
     *
     * @param context The Android application context.
     * @param accountId The unique account identifier.
     * @return An instance of [InAppAssetsStore].
     */
    fun provideInAppAssetsStore(
        context: Context,
        accountId: String
    ): InAppAssetsStore {
        val prefName = constructStorePreferenceName(storeType = STORE_TYPE_INAPP_ASSETS, accountId = accountId)
        return InAppAssetsStore(getCTPreference(context, prefName))
    }
    fun provideFileStore(
        context: Context,
        accountId: String
    ): FileStore {
        val prefName = constructStorePreferenceName(storeType = STORE_TYPE_FILES, accountId = accountId)
        return FileStore(getCTPreference(context, prefName))
    }

    /**
     * Provides an instance of [InAppStore] using the given parameters.
     *
     * @param context The Android application context.
     * @param cryptHandler The handler used for encryption and decryption of In-App messages.
     * @param deviceId The device id for user.
     * @param accountId The unique account identifier.
     * @return An instance of [InAppStore].
     */
    fun provideInAppStore(
        context: Context,
        cryptHandler: CryptHandler,
        deviceId: String,
        accountId: String
    ): InAppStore {
        val prefName = constructStorePreferenceName(STORE_TYPE_INAPP, deviceId, accountId)
        return InAppStore(getCTPreference(context, prefName), cryptHandler)
    }

    /**
     * Provides an instance of [ImpressionStore] using the given parameters.
     *
     * @param context The Android application context.
     * @param deviceId The device id for user.
     * @param accountId The unique account identifier.
     * @return An instance of [ImpressionStore].
     */
    fun provideImpressionStore(
        context: Context,
        deviceId: String,
        accountId: String
    ): ImpressionStore {
        val prefName = constructStorePreferenceName(STORE_TYPE_IMPRESSION, deviceId, accountId)
        return ImpressionStore(getCTPreference(context, prefName))
    }

    /**
     * Provides an instance of [LegacyInAppStore] using the given parameters.
     *
     * @param context The Android application context.
     * @param accountId The unique account identifier.
     * @return An instance of [LegacyInAppStore].
     */
    fun provideLegacyInAppStore(context: Context, accountId: String): LegacyInAppStore {
        val prefName = constructStorePreferenceName(STORE_TYPE_LEGACY_INAPP)
        return LegacyInAppStore(getCTPreference(context, prefName), accountId)
    }

    /**
     * Gets an instance of [CTPreference] for the specified [prefName].
     *
     * @param context The Android application context.
     * @param prefName The name of the preference.
     * @return An instance of [CTPreference].
     */
    @VisibleForTesting
    fun getCTPreference(context: Context, prefName: String) = CTPreference(context, prefName)

    /**
     * Constructs the preference name based on the store type, device ID, and account ID.
     *
     * @param storeType The type of store (e.g., In-App, Impression, etc.).
     * @param deviceId The unique device identifier.
     * @param accountId The unique account identifier.
     * @return The constructed preference name.
     */
    fun constructStorePreferenceName(storeType: Int, deviceId: String = "", accountId: String = ""): String =
        when (storeType) {
            STORE_TYPE_INAPP_ASSETS -> "$ASSET_STORE_PREFIX:$accountId"
            STORE_TYPE_FILES -> "$FILE_STORE_PREFIX:$accountId"
            STORE_TYPE_INAPP -> "${Constants.INAPP_KEY}:$deviceId:$accountId"
            STORE_TYPE_IMPRESSION -> "${Constants.KEY_COUNTS_PER_INAPP}:$deviceId:$accountId"
            STORE_TYPE_LEGACY_INAPP -> Constants.CLEVERTAP_STORAGE_TAG
            else -> Constants.CLEVERTAP_STORAGE_TAG
        }
}

