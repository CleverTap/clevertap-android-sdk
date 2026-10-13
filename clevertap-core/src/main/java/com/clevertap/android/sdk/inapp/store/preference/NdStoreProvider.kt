package com.clevertap.android.sdk.inapp.store.preference

import android.content.Context
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.StoreProvider

/**
 * Lazily creates and caches the Native Display stores ([NdStore] + ND [ImpressionStore] +
 * [NdCountsStore]), keyed by device id. Each is built on first access once the device id resolves and
 * rebuilt when it changes, so this is the single user-switch owner for every persistent ND store (none
 * is a `ChangeUserCallback`). Stores are null until the device id resolves — ND is simply inactive then.
 */
internal class NdStoreProvider(
    private val context: Context,
    private val deviceInfo: DeviceInfo,
    private val accountId: String,
    private val storeProvider: StoreProvider = StoreProvider.getInstance(),
) {

    private var cachedDeviceId: String? = null
    private var ndStoreCache: NdStore? = null
    private var ndImpressionStoreCache: ImpressionStore? = null
    private var ndCountsStoreCache: NdCountsStore? = null

    val ndStore: NdStore?
        get() = ensureForCurrentUser().let { ndStoreCache }

    val ndImpressionStore: ImpressionStore?
        get() = ensureForCurrentUser().let { ndImpressionStoreCache }

    val ndCountsStore: NdCountsStore?
        get() = ensureForCurrentUser().let { ndCountsStoreCache }

    /** (Re)creates the stores if the device id is now available or has changed since last access. */
    @Synchronized
    private fun ensureForCurrentUser() {
        val deviceId = deviceInfo.deviceID ?: return // ND is inactive until the device id resolves
        if (deviceId == cachedDeviceId && ndStoreCache != null) {
            return
        }
        cachedDeviceId = deviceId
        ndStoreCache = storeProvider.provideNdStore(context, deviceId, accountId)
        ndImpressionStoreCache = storeProvider.provideNdImpressionStore(context, deviceId, accountId)
        ndCountsStoreCache = storeProvider.provideNdCountsStore(context, deviceId, accountId)
    }
}
