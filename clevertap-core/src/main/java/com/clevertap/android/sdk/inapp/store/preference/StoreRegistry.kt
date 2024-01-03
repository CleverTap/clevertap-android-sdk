package com.clevertap.android.sdk.inapp.store.preference

/**
 * The `StoreRegistry` data class acts as a registry for various stores related to In-App messaging.
 * It holds instances of different store types, allowing easy access to these stores throughout the application.
 *
 * @property inAppStore An instance of [InAppStore] for storing In-App messages.
 * @property impressionStore An instance of [ImpressionStore] for tracking impressions of In-App messages.
 * @property legacyInAppStore An instance of [LegacyInAppStore] for storing legacy In-App messages.
 * @property inAppAssetsStore An instance of [InAppAssetsStore] for handling In-App assets storage.
 *
 * @constructor Creates a StoreRegistry with optional instances of different stores.
 */
data class StoreRegistry constructor(
    var inAppStore: InAppStore? = null,
    var impressionStore: ImpressionStore? = null,
    var legacyInAppStore: LegacyInAppStore? = null,
    var inAppAssetsStore: InAppAssetsStore? = null
)