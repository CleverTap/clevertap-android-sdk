package com.clevertap.android.sdk.inapp.store.preference

import android.content.Context
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.StoreProvider

/**
 * Lazily creates and caches the Native Display stores ([NdStore] + ND [ImpressionStore]), keyed by
 * device id (SDK-6055).
 *
 * ND stores can't be built at factory time because they're namespaced by the device id, which resolves
 * asynchronously. Instead of the previous pattern (nullable [StoreRegistry] fields created in two
 * places — `CleverTapFactory.initStores` and `CleverTapAPI.deviceIDCreated` — plus change-user
 * callbacks to repoint them), this provider creates each store on first access once the device id is
 * available and transparently rebuilds them when the user (device id) changes. Callers always read
 * through [StoreRegistry], so the swap is invisible to them.
 *
 * The stores remain nullable until the device id resolves — that's not a wiring smell, it's ND simply
 * being inactive before the SDK has an identity.
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

    val ndStore: NdStore?
        get() = ensureForCurrentUser().let { ndStoreCache }

    val ndImpressionStore: ImpressionStore?
        get() = ensureForCurrentUser().let { ndImpressionStoreCache }

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
    }
}
